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
package org.frankframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

/**
 * Custom implementation of an {@link HttpEntity} which deals with {@link #isRepeatable()},
 * {@link #isStreaming()}, {@link Message#getCharset()} and {@link Message#size()}.
 *
 * @author Niels Meijer
 */
@Log4j2
public class HttpMessageEntity extends AbstractHttpEntity {
	private final Message message;
	private long contentLength;

	public HttpMessageEntity(Message message) {
		this(message, null);
	}

	public HttpMessageEntity(Message message, ContentType contentType) {
		this.message = message;
		// Pre-compute this, because we will always anyway need it, and we cannot access it after writing the message (which breaks some tests).
		this.contentLength = computeContentLength();

		String charset = message.getCharset();
		if (contentType != null) {
			Charset contentTypeCharset = contentType.getCharset();
			if (contentTypeCharset != null) {
				if (StringUtils.isNotEmpty(charset) && !contentTypeCharset.name().equalsIgnoreCase(charset)) {
					// Only log warning when message charset has explicitly been set and is not equals to the ContentType.
					log.warn("overriding Message charset [{}] with value supplied from content-type [{}]", message::getCharset, contentTypeCharset::name);
				}
				charset = contentTypeCharset.name();
			} else {
				contentType.withCharset(charset);
			}
			setContentType(contentType.toString());
		}
		setContentEncoding(charset);
	}

	@Override // Overridden because we don't want to set empty values
	public void setContentEncoding(String charset) {
		if(charset == null || !charset.isEmpty()) {
			super.setContentEncoding(charset);
		}
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public boolean isStreaming() {
		return message.requiresStream();
	}

	@Override
	public long getContentLength() {
		return contentLength;
	}

	private long computeContentLength() {
		long messageSize = message.size();
		// To get an accurate value if the size is unknown we need to check if data is available.
		if (messageSize == Message.MESSAGE_SIZE_UNKNOWN && message.isEmpty()) {
			return 0L;
		}
		return messageSize;
	}

	// size (getContentLength) and encoding (getContentEncoding) of the InputStream must match the way it is being read / sent!
	@Override
	public InputStream getContent() throws IOException {
		return message.asInputStream(message.getCharset());
	}

	@Override
	public void writeTo(OutputStream outStream) throws IOException {
		long length = getContentLength();
		try (InputStream inStream = getContent()) {
			// consume no more than length. Update contentLength because it may have been unknown before reading the stream, now it is accurate.
			contentLength = StreamUtil.copyPartialStream(inStream, outStream, length, OUTPUT_BUFFER_SIZE);
		}
	}
}
