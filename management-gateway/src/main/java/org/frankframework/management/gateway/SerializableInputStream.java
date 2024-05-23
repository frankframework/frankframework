/*
   Copyright 2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.management.gateway;

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SerializableInputStream extends InputStream implements Externalizable {

	private static final long serialVersionUID = 1L;
	private transient static final int BUFFERSIZE = 20_480;
	private transient static final Logger LOG = LogManager.getLogger(SerializableInputStream.class);

	private transient volatile InputStream delegate;
	private transient final Path tmpFile;

	/**
	 * Constructor for deserialization.
	 */
	public SerializableInputStream() {
		super();

		LOG.trace("creating new SerializableInputStream from deserialization");
		String tmpdir = System.getProperty("java.io.tmpdir");
		Path dir = Path.of(tmpdir);

		Path tmpDir = dir.resolve("frank-management-gateway");
		try {
			if(!Files.exists(tmpDir)) {
				Files.createDirectory(tmpDir);
			}

			tmpFile = File.createTempFile("msg", ".dat", tmpDir.toFile()).toPath();
			LOG.trace("determined temporary file location [{}]", tmpFile);
		} catch (IOException e) {
			throw new IllegalStateException("unable to create temp file to de-serialize stream", e);
		}
	}

	/**
	 * Default constructor, to wrap the original stream in.
	 */
	public SerializableInputStream(final InputStream in) {
		super();

		LOG.trace("creating new SerializableInputStream with delegate");
		delegate = in;
		tmpFile = null;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		LOG.trace("Writing delegate to ObjectOutput with buffersize [{}]", BUFFERSIZE);

		try(InputStream is = delegate) {
			int overallBytes = 0;
			final byte[] buffer = new byte[BUFFERSIZE];
			int bytesRead;
			while ((bytesRead = is.read(buffer, 0, BUFFERSIZE)) > 0) {
				LOG.trace("wrote [{}] bytes to ObjectOutput", bytesRead);
				out.writeInt(bytesRead);
				out.write(buffer, 0, bytesRead);
				overallBytes += bytesRead;
			}
			out.writeInt(0);
			LOG.trace("finished serializing stream, total bytes written [{}]", overallBytes);
		} finally {
			delegate = null;
		}
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		try (OutputStream fos = Files.newOutputStream(tmpFile)) {
			copyStream(in, fos);
		}

		delegate = Files.newInputStream(tmpFile);
	}

	private void copyStream(ObjectInput in, OutputStream fos) throws IOException {
		int overallBytes = 0;
		int toRead = in.readInt();
		overallBytes += toRead;
		byte[] buffer = null;
		while (toRead > 0) {
			LOG.trace("reading up to [{}] bytes from ObjectInput", toRead);
			buffer = allocateBuffer(toRead, buffer);
			final int readLocally = in.read(buffer, 0, toRead);
			LOG.trace("read [{}] bytes from stream and stored them in the buffer", readLocally);
			fos.write(buffer, 0, readLocally);

			toRead -= readLocally;
			if (toRead == 0) {
				toRead = in.readInt();
				overallBytes += toRead;
			}
		}
		LOG.trace("finished deserializing stream, total bytes read [{}]", overallBytes);
	}

	/**
	 * Initially creates a buffer equal to the length of the first bytes 'part', set by {@link SerializableInputStream#BUFFERSIZE}.
	 */
	private byte[] allocateBuffer(final int size, final byte[] buffer) {
		if (buffer != null && buffer.length >= size) {
			return buffer;
		}
		return new byte[size];
	}

	@Override
	public int read() throws IOException {
		if(delegate == null) {
			throw new IOException("error delegate has not been set");
		}

		return delegate.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		if(delegate == null) {
			throw new IOException("error delegate has not been set");
		}

		return delegate.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if(delegate == null) {
			throw new IOException("error delegate has not been set");
		}

		return delegate.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		try {
			if(delegate != null) {
				delegate.close();
			}
		} finally {
			if(tmpFile != null) {
				LOG.trace("removing temporary file location [{}]", tmpFile);
				Files.delete(tmpFile);
			}
		}
	}
}
