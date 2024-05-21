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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

//TODO figure out if we want to keep using this or not...
public class SerializableInputStream extends InputStream implements Serializable {
	private static final int BUFFERSIZE = 20_480;
	private volatile InputStream delegate;

	public SerializableInputStream() {
		super();
	}

	public SerializableInputStream(InputStream in) {
		super();
		delegate = in;
	}

	private void writeObject(final ObjectOutputStream out) throws IOException {
		try(InputStream is = delegate) {
			byte[] buffer=new byte[BUFFERSIZE];
			int bytesRead;
			while ((bytesRead=is.read(buffer, 0, BUFFERSIZE)) > -1) {
				out.write(buffer, 0, bytesRead);
			}
		} finally {
			delegate = null;
		}
	}

	private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
		byte[] obj = (byte[]) in.readObject();
		delegate = new ByteArrayInputStream(obj);
	}

	@Override
	public int read() throws IOException {
		if(delegate == null) {
			throw new IOException("error reading delegate");
		}

		return delegate.read();
	}

	@Override
	public int read(byte[] b) throws IOException {
		if(delegate == null) {
			throw new IOException("error reading delegate");
		}

		return delegate.read(b);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if(delegate == null) {
			throw new IOException("error reading delegate");
		}

		return delegate.read(b, off, len);
	}

	@Override
	public void close() throws IOException {
		if(delegate == null) {
			throw new IOException("error closing delegate");
		}
		delegate.close();
	}
}
