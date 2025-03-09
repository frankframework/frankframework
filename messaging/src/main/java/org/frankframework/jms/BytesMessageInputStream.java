/*
   Copyright 2021-2023 WeAreFrank!

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
package org.frankframework.jms;

import java.io.IOException;
import java.io.InputStream;

import jakarta.annotation.Nonnull;
import jakarta.jms.BytesMessage;
import jakarta.jms.JMSException;

public class BytesMessageInputStream extends InputStream {

	private final BytesMessage bytesMsg;

	public BytesMessageInputStream(BytesMessage bytesMsg) {
		this.bytesMsg = bytesMsg;
	}

	@Override
	public int read() throws IOException {
		try {
			byte[] data = new byte[1];
			int len = bytesMsg.readBytes(data);
			if (len == -1) {
				return -1;
			}
			// Make the return-value an unsigned byte to honour the contract of InputStream#read()
			return data[0] & 0xFF;
		} catch (JMSException e) {
			throw new IOException("Cannot read JMS message", e);
		}
	}

	@Override
	public int read(@Nonnull byte[] b) throws IOException {
		try {
			return bytesMsg.readBytes(b);
		} catch (JMSException e) {
			throw new IOException("Cannot read JMS message", e);
		}
	}

	@Override
	public int read(@Nonnull byte[] b, int off, int len) throws IOException {
		try {
			// If we try filling the whole array from start, then we do not need to read data into
			// a temp array and copy it.
			if (off == 0 && len == b.length) {
				return read(b);
			}
			byte[] data = new byte[len];
			int result = bytesMsg.readBytes(data);
			if (result > 0) {
				System.arraycopy(data, 0, b, off, result);
			}
			return result;
		} catch (JMSException e) {
			throw new IOException("Cannot read JMS message", e);
		}
	}
}
