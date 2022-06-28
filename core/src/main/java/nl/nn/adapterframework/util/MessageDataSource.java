/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

import org.springframework.util.MimeType;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;

public class MessageDataSource implements DataSource {
	private final Message message;
	private final String name;

	public MessageDataSource(Message message) throws IOException {
		if(message.isNull()) {
			throw new IllegalArgumentException("message may not be null");
		}
		if(message.getContext() == null) {
			throw new IllegalArgumentException("no message context available");
		}

		this.message = message;
		this.message.preserve();
		this.name = (String) message.getContext().get(MessageContext.METADATA_NAME);
	}

	/**
	 * Use content type application/octet-stream in case it cannot be
	 * determined. See http://docs.oracle.com/javase/7/docs/api/javax/activation/DataSource.html#getContentType():
	 * This method returns the MIME type of the data in the form of a string.
	 * It should always return a valid type. It is suggested that getContentType
	 * @returns "application/octet-stream" if the DataSource implementation can
	 * not determine the data type.
	 */
	@Override
	public String getContentType() {
		if(message.isBinary()) {
			MimeType mimeType = MessageUtils.computeMimeType(message, getName());
			if(mimeType != null) {
				return mimeType.toString();
			}
		}
		return "application/octet-stream";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return message.asInputStream();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}
}
