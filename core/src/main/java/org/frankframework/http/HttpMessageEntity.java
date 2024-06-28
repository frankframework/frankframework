/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

/**
 * Custom implementation of an {@link HttpEntity} which deals with {@link #isRepeatable()},
 * {@link #isStreaming()}, {@link Message#getCharset()} and {@link Message#size()}.
 *
 * @author Niels Meijer
 */
public class HttpMessageEntity extends AbstractHttpEntity {
	private final Logger log = LogUtil.getLogger(this);
	private final Message message;

	public HttpMessageEntity(Message message) {
		this(message, null);
	}

	public HttpMessageEntity(Message message, ContentType contentType) {
		this.message = message;

		String charset = message.getCharset();
		if(contentType != null) {
			Charset contentTypeCharset = contentType.getCharset();
			if(contentTypeCharset != null) {
				if(!contentTypeCharset.name().equalsIgnoreCase(charset)) {
					log.warn("overriding Message [{}] charset with value supplied from content-type [{}]", message::getCharset, contentTypeCharset::name);
				}
				charset = contentTypeCharset.name();
			} else {
				contentType.withCharset(charset);
			}
			setContentType(contentType.toString());
		}
		setContentEncoding(charset);
	}

	@Override //overridden because we don't want to set empty values
	public void setContentEncoding(String charset) {
		if(charset == null || charset.length() > 0) {
			super.setContentEncoding(charset);
		}
	}

	@Override
	public boolean isRepeatable() {
		return message.isRepeatable();
	}

	@Override
	public boolean isStreaming() {
		return message.requiresStream();
	}

	@Override
	public long getContentLength() {
		return message.size();
	}

	// size (getContentLength) and encoding (getContentEncoding) of the InputStream must match the way it is being read / sent!
	@Override
	public InputStream getContent() throws IOException {
		return message.asInputStream(message.getCharset());
	}

	@Override
	public void writeTo(OutputStream outStream) throws IOException {
		int length = Math.toIntExact(getContentLength());
		try (InputStream inStream = getContent()) {
			final byte[] buffer = new byte[OUTPUT_BUFFER_SIZE];
			int readLen;
			if(length < 0) {
				// consume until EOF
				while((readLen = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, readLen);
				}
			} else {
				// consume no more than length
				long remaining = length;
				while(remaining > 0) {
					readLen = inStream.read(buffer, 0, (int) Math.min(OUTPUT_BUFFER_SIZE, remaining));
					if(readLen == -1) {
						break;
					}
					outStream.write(buffer, 0, readLen);
					remaining -= readLen;
				}
			}
		}
	}
}
