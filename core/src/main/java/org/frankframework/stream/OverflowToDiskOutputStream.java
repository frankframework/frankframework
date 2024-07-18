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
package org.frankframework.stream;

import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * Writes to an in-memory buffer until it 'overflows', after which a file on disk will be created and the in-memory buffer will be flushed to it.
 */
public class OverflowToDiskOutputStream extends OutputStream implements AutoCloseable, Flushable {
	private byte[] buffer; // temporary buffer, once full, write to disk
	private OutputStream outputStream;

	private final Path tempDirectory;
	private Path fileLocation;
	private boolean closed = false;

	/**
	 * The number of bytes in the buffer. This value is always in the range
	 * {@code 0} through {@code buf.length}; elements {@code buf[0]} through
	 * {@code buf[count-1]} contain valid byte data.
	 * 
	 * When count equals {@link #MAX_IN_MEMORY_SIZE} the buffer will be flushed to the {@link OutputStream OutputStream out}.
	 */
	private int count = 0;

	public OverflowToDiskOutputStream(int bufferSize, Path tempDirectory) throws IOException {
		this.tempDirectory = tempDirectory;

		// either the buffer or outputStream exists, but not both at the same time.
		if (bufferSize > 0) {
			buffer = new byte[bufferSize];
		} else {
			outputStream = createFileOnDisk();
		}
	}

	private OutputStream createFileOnDisk() throws IOException {
		fileLocation = Files.createTempFile(tempDirectory, "msg", "dat");
		return Files.newOutputStream(fileLocation);
	}

	private OutputStream flushBufferToDisk() throws IOException {
		if (count == 0 && outputStream != null) { //buffer has been reset, and fos exists.
			return outputStream;
		}

		// create the OutputStream and write the buffer to it.
		OutputStream fos = createFileOnDisk();
		fos.write(buffer, 0, count);

		// empty the buffer, there is no need to keep this in memory any longer.
		buffer = null;
		count = 0;

		return fos;
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] {(byte) b}, 0, 1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(closed) throw new IllegalStateException("stream already closed");

		// Buffer has already been flushed
		if(outputStream != null) {
			outputStream.write(b, off, len);
			return;
		}

		// If the request length exceeds the size of the output buffer, flush the output buffer and then write the data directly.
		if (len >= buffer.length - count) {
			outputStream = flushBufferToDisk();
			outputStream.write(b, off, len);
			return;
		}

		// Write to the buffer
		System.arraycopy(b, off, buffer, count, len);
		count += len;
	}

	/**
	 * If the contents was small enough to be kept in memory a ByteArray-message will be returned.
	 * If the contents was written to disk a {@link PathMessage TemporaryMessage} will be returned.
	 * @return A new {@link Message} object representing the contents written to this {@link OutputStream}.
	 */
	public Message toMessage() {
		if(!closed) throw new IllegalStateException("stream has not yet been closed");

		if(fileLocation != null) {
			return PathMessage.asTemporaryMessage(fileLocation);
		} else {
			return new Message(Arrays.copyOf(buffer, count));
		}
	}

	/** 
	 * Doesn't do anything if the message is kept in memory
	 */
	@Override
	public void flush() throws IOException {
		flush(false);
	}

	/**
	 * Doesn't do anything if the message is kept in memory unless parameter {@code writeBufferToDisk} is true.
	 * @param writeBufferToDisk Forces the in-memory buffer to be written to disk.
	 */
	public void flush(boolean writeBufferToDisk) throws IOException {
		if(writeBufferToDisk) {
			outputStream = flushBufferToDisk();
		}
		if(outputStream != null) {
			outputStream.flush();
		}
	}

	/**
	 * Flushes and closes the OutputStream, clears all resources, does not remove the file if has been created on disk.
	 */
	@Override
	public void close() throws IOException {
		if(closed) {
			throw new IllegalStateException("already closed");
		}
		closed = true;

		if(outputStream != null) {
			outputStream.close();
		}
	}
}
