/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.http.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.AbstractContentBody;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.util.LogUtil;

public class MessageContentBody extends AbstractContentBody {
	private Logger log = LogUtil.getLogger(this);
	private final Message message;
	private String filename;
	private static final int OUTPUT_BUFFER_SIZE = 4096;

	public MessageContentBody(Message message) {
		this(message, message.isBinary() ? ContentType.APPLICATION_OCTET_STREAM : ContentType.DEFAULT_TEXT);
	}

	public MessageContentBody(Message message, ContentType contentType) {
		this(message, contentType, null);
	}

	public MessageContentBody(Message message, ContentType contentType, String filename) {
		super(contentType);
		this.message = message;
		Map<String, Object> context = message.getContext();
		if(context != null && filename == null) {
			this.filename = (String) context.get(MessageContext.METADATA_NAME);
		}
		log.debug("creating part from message ["+message+"] name ["+filename+"] contentType ["+contentType+"]");
	}

	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		int length = Math.toIntExact(getContentLength());
		if(message.requiresStream()) {
			try (InputStream inStream = message.asInputStream()) {
				final byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
				int readLen;
				if(length < 0) {
					// consume until EOF
					while((readLen = inStream.read(buffer)) != -1) {
						out.write(buffer, 0, readLen);
					}
				} else {
					// consume no more than length
					long remaining = length;
					while(remaining > 0) {
						readLen = inStream.read(buffer, 0, (int) Math.min(OUTPUT_BUFFER_SIZE, remaining));
						if(readLen == -1) {
							break;
						}
						out.write(buffer, 0, readLen);
						remaining -= readLen;
					}
				}
			}
		} else {
			out.write(message.asByteArray(), 0, length);
			out.flush();
		}
	}

	@Override
	public String getTransferEncoding() {
		return (message.isBinary()) ? MIME.ENC_BINARY : MIME.ENC_8BIT;
	}

	@Override
	public long getContentLength() {
		return message.size();
	}

}
