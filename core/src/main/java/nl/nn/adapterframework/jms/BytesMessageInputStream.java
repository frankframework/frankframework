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
package nl.nn.adapterframework.jms;

import java.io.IOException;
import java.io.InputStream;

import javax.jms.BytesMessage;
import javax.jms.JMSException;

public class BytesMessageInputStream extends InputStream {

	private final BytesMessage bytesMsg;

	public BytesMessageInputStream(BytesMessage bytesMsg) {
		this.bytesMsg = bytesMsg;
	}

	@Override
	public int read() throws IOException {
		try {
			return bytesMsg.readByte();
		} catch (JMSException e) {
			throw new IOException("Cannot read JMS message", e);
		}
	}

	@Override
	public int read(byte[] b) throws IOException {
		try {
			return bytesMsg.readBytes(b);
		} catch (JMSException e) {
			throw new IOException("Cannot read JMS message", e);
		}
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			byte[] readbuf = new byte[len];
			int result = bytesMsg.readBytes(readbuf);
			if (result > 0) {
				System.arraycopy(readbuf, 0, b, off, result);
			}
			return result;
		} catch (JMSException e) {
			throw new IOException("Cannot read JMS message", e);
		}
	}
}
