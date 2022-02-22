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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;

public abstract class MessageUtils {
	private static Logger LOG = LogUtil.getLogger(MessageUtils.class);

	/**
	 * Fetch metadata from the {@link HttpServletRequest} such as Content-Length, Content-Type (mimetype + charset)
	 */
	public static MessageContext getContext(HttpServletRequest request) {
		MessageContext result = new MessageContext();
		result.withCharset(request.getCharacterEncoding());
		int contentLength = request.getContentLength();
		result.withSize(contentLength);
		result.withMimeType(request.getContentType());

		Enumeration<String> names = request.getHeaderNames();
		while(names.hasMoreElements()) {
			String name = names.nextElement();
			result.put(name, request.getHeader(name));
		}

		return result;
	}

	/**
	 * If content is present (POST/PUT) one of the following headers must be set:<br/>
	 * Content-Length / Transfer-Encoding <br/>
	 * If neither header is present a <code>nullMessage</code> will be returned.
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc7230#section-3.3">rfc7230</a>
	 */
	public static Message parseContentAsMessage(HttpServletRequest request) throws IOException {
		if(request.getContentLength() > -1 || request.getHeader("transfer-encoding") != null) {
			return new Message(request.getInputStream(), getContext(request));
		} else {
			return Message.nullMessage();
		}
	}

	public static String computeContentType(Message message) {
		if(Message.isEmpty(message) || message.getContext() == null) {
			return null;
		}

		MimeType mimeType = (MimeType)message.getContext().get(MessageContext.METADATA_MIMETYPE);
		if(mimeType == null) {
			return null;
		}

		StringBuilder contentType = new StringBuilder();
		contentType.append(mimeType.getType());
		contentType.append('/');
		contentType.append(mimeType.getSubtype());

		if(message.getCharset() != null) {
			contentType.append(";charset=");
			contentType.append(message.getCharset());
		}
		return contentType.toString();
	}

	public static MimeType getMimeType(Message message) {
		Map<String, Object> context = message.getContext();
		return (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
	}

	public static MimeType computeMimeType(Message message, String filename) {
		Map<String, Object> context = message.getContext();
		MimeType mimeType = getMimeType(message);
		if(mimeType != null) {
			return mimeType;
		}

		String name = (String) context.get(MessageContext.METADATA_NAME);
		if(StringUtils.isNotEmpty(filename)) {
			name = filename;
		}

		try {
			message.preserve();
			TikaConfig tika = new TikaConfig();
			Metadata metadata = new Metadata();
			metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, name);
			org.apache.tika.mime.MediaType tikaMediaType = tika.getDetector().detect(message.asInputStream(), metadata);
			return MimeType.valueOf(tikaMediaType.toString());
		} catch (Throwable t) {
			LOG.warn("error parsing message to determine mimetype", t);
		}

		LOG.info("unable to determine mimetype");
		return null;
	}
}
