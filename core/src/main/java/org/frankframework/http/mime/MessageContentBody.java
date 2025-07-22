/*
   Copyright 2022-2025 WeAreFrank!

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
package org.frankframework.http.mime;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.content.ContentBody;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.StreamUtil;

@Log4j2
public class MessageContentBody implements ContentBody {
	private final Message message;
	private final String filename;
	private static final int OUTPUT_BUFFER_SIZE = 4096;
	private final MimeType mimeType;

	public MessageContentBody(Message message) {
		this(message, null);
	}

	public MessageContentBody(Message message, MimeType contentType) {
		this(message, contentType, null);
	}

	public MessageContentBody(Message message, MimeType contentType, String filename) {
		this.message = message;
		this.filename = filename;

		MimeType type = contentType != null ? contentType : MessageUtils.getMimeType(message);
		if(type == null) {
			type = message.isBinary() ? MediaType.APPLICATION_OCTET_STREAM : MediaType.TEXT_PLAIN;
		}
		this.mimeType = type;

//		Map<String, Object> context = message.getContext();
//		if(context != null && filename == null) {
//			this.filename = (String) context.get(MessageContext.METADATA_NAME); // This might trigger issue #3917, introducing a filename where it must be empty.
//		}
		log.debug("creating part from message [{}] name [{}] contentType [{}]", message, filename, contentType);
	}

	@Override
	public String getFilename() {
		return filename;
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		long length = getContentLength();
		try (InputStream inStream = message.asInputStream(getCharset())) {
			// consume no more than length
			StreamUtil.copyPartialStream(inStream, out, length, OUTPUT_BUFFER_SIZE);
		}
	}

	@Override
	public String getTransferEncoding() {
		return message.isBinary() ? MIME.ENC_BINARY : MIME.ENC_8BIT;
	}

	@Override
	public long getContentLength() {
		return message.size();
	}

	@Override
	public String getMimeType() {
		return getMediaType() + '/' + getSubType();
	}

	@Override
	public String getMediaType() {
		return mimeType.getType();
	}

	@Override
	public String getSubType() {
		return mimeType.getSubtype();
	}

	@Override
	public String getCharset() {
		if(message.isBinary()) {
			return null;
		}

		Charset charset = mimeType.getCharset() != null ? mimeType.getCharset() : StreamUtil.DEFAULT_CHARSET;
		if(charset != null) {
			return charset.name();
		}
		return null;
	}
}
