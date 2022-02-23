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
import java.nio.charset.Charset;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;

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
		if (contentLength>=0) {
			result.withSize(contentLength);
		}

		String contentType = request.getContentType();
		if(StringUtils.isNotEmpty(contentType)) {
			try {
				ContentType parsedContentType = ContentType.parse(contentType);
				if(parsedContentType.getMimeType() != null) {
					result.withMimeType(parsedContentType.getMimeType());
				}
				Charset parsedCharset = parsedContentType.getCharset();
				if(parsedCharset != null) {
					result.withCharset(parsedCharset.displayName());
				}
			} catch (Exception e) {
				//For now just log when we cannot parse, perhaps we should abort the request?
				LOG.warn("unable to parse charset from contentType [{}]", contentType, e);
			}
		}

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

		String mimeType = (String)message.getContext().get(MessageContext.METADATA_MIMETYPE);
		if(StringUtils.isEmpty(mimeType)) {
			return null;
		}

		StringBuilder contentType = new StringBuilder(mimeType);
		if(!message.isBinary()) {
			contentType.append(";charset=");
			contentType.append(message.getCharset());
		}
		return contentType.toString();
	}
}
