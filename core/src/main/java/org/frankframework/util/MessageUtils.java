/*
   Copyright 2022-2024 WeAreFrank!

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
package org.frankframework.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.MimeHeader;
import jakarta.xml.soap.SOAPException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.Logger;
import org.apache.tika.Tika;
import org.springframework.util.DigestUtils;
import org.springframework.util.MimeType;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import org.frankframework.receivers.MessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;

public class MessageUtils {

	private MessageUtils() {
		throw new IllegalStateException("Don't construct utility class");
	}

	private static final Logger LOG = LogUtil.getLogger(MessageUtils.class);
	private static final int CHARSET_CONFIDENCE_LEVEL = AppConstants.getInstance().getInt("charset.confidenceLevel", 65);
	private static final Tika TIKA = new Tika();

	/**
	 * Fetch metadata from the {@link HttpServletRequest} such as Content-Length, Content-Type (mimetype + charset)
	 */
	public static MessageContext getContext(HttpServletRequest request) {
		MessageContext result = new MessageContext();
		result.withCharset(request.getCharacterEncoding());
		int contentLength = request.getContentLength();
		result.withSize(contentLength);
		String contentType = request.getContentType();
		if(StringUtils.isNotEmpty(contentType)) {
			result.withMimeType(request.getContentType());
		}

		Enumeration<String> names = request.getHeaderNames();
		while(names.hasMoreElements()) {
			String name = names.nextElement();
			// https://datatracker.ietf.org/doc/html/rfc7230
			// Each header field consists of a case-insensitive field name followed by a colon (":"),
			// optional leading whitespace, the field value, and optional trailing whitespace.
			result.put(MessageContext.HEADER_PREFIX + name, request.getHeader(name).trim());
		}

		return result;
	}

	public static MessageContext getContext(Iterator<MimeHeader> mimeHeaders) {
		MessageContext result = new MessageContext();
		while (mimeHeaders.hasNext()) {
			MimeHeader header = mimeHeaders.next();
			String name = header.getName();
			if(HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(name)) {
				result.withMimeType(header.getValue());
			} else {
				result.put(MessageContext.HEADER_PREFIX + name, header.getValue());
			}
		}
		return result;
	}

	public static MessageContext getContext(HttpResponse httpResponse) {
		MessageContext result = new MessageContext();
		HttpEntity entity = httpResponse.getEntity();
		if(entity != null) {
			result.withSize(entity.getContentLength());
			Header contentType = entity.getContentType();
			if(contentType != null) {
				result.withMimeType(contentType.getValue());
			}
		} else {
			Header contentTypeHeader = httpResponse.getFirstHeader(HttpHeaders.CONTENT_TYPE);
			if(contentTypeHeader != null) {
				result.withMimeType(contentTypeHeader.getValue());
			}
		}

		if (httpResponse.getAllHeaders() != null) {
			for(Header header: httpResponse.getAllHeaders()) {
				String name = header.getName();
				result.put(MessageContext.HEADER_PREFIX + name, header.getValue());
			}
		}

		return result;
	}

	/**
	 * If content is present (POST/PUT) one of the following headers must be set:<br/>
	 * Content-Length / Transfer-Encoding <br/>
	 * If neither header is present, or the size is <code>0</code> a <code>nullMessage</code> will be returned.
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc7230#section-3.3">rfc7230</a>
	 */
	public static Message parseContentAsMessage(HttpServletRequest request) throws IOException {
		if(request.getContentLength() > 0 || request.getHeader("transfer-encoding") != null) {
			return new Message(request.getInputStream(), getContext(request));
		} else {
			// We want the context because of the request headers
			return Message.nullMessage(getContext(request));
		}
	}

	public static Message parse(AttachmentPart soapAttachment) throws SOAPException {
		return new Message(soapAttachment.getRawContentBytes(), getContext(soapAttachment.getAllMimeHeaders()));
	}

	/**
	 * Reads the first 10k bytes of (binary) messages to determine the charset when not present in the {@link MessageContext}.
	 * @throws IOException when it cannot read the first 10k bytes.
	 */
	public static Charset computeDecodingCharset(Message message) throws IOException {
		return computeDecodingCharset(message, CHARSET_CONFIDENCE_LEVEL);
	}

	/**
	 * Reads the first 10k bytes of (binary) messages to determine the charset when not present in the {@link MessageContext}.
	 * @param confidence percentage required to successfully determine the charset.
	 * @throws IOException when it cannot read the first 10k bytes.
	 */
	public static Charset computeDecodingCharset(Message message, int confidence) throws IOException {
		if(Message.isEmpty(message) || !message.isBinary()) {
			return null;
		}

		if(StringUtils.isNotEmpty(message.getCharset()) && !StreamUtil.AUTO_DETECT_CHARSET.equalsIgnoreCase(message.getCharset())) {
			return Charset.forName(message.getCharset());
		}

		CharsetDetector detector = new CharsetDetector();
		detector.setText(message.asInputStream());
		CharsetMatch match = detector.detect();
		String charset = match.getName();

		if(match.getConfidence() > 90) {
			LOG.debug("update charset for message [{}], full match [{}] with confidence level [{}/{}]", message, charset, match.getConfidence(), confidence);
			return updateMessageCharset(message, charset);
		}

		//Guesstimate, encoding is not UTF-8 but either CP1252/Latin1/ISO-8859-1.
		if(charset.startsWith("windows-125")) {
			charset = "windows-1252";//1250/1/3 have a combined adoption rate of 1.6% assume 1252 instead!
		}
		if(match.getConfidence() >= confidence) {
			LOG.debug("update charset for message [{}], potential match [{}] with confidence level [{}/{}]", message, charset, match.getConfidence(), confidence);
			return updateMessageCharset(message, charset);
		}

		LOG.info("unable to detect charset for message [{}] closest match [{}] did not meet confidence level [{}/{}]", message, charset, match.getConfidence(), confidence);
		return updateMessageCharset(message, null); //return NULL so calling method can fall back to the default charset.
	}

	//Update the MessageContext charset field, it may not remain StreamUtil.AUTO_DETECT_CHARSET
	private static Charset updateMessageCharset(Message message, String charsetName) {
		try {
			if(charsetName != null) {
				return Charset.forName(charsetName); //parse it first to validate the charset
			}
			return null;
		} finally {
			MessageContext context = message.getContext();
			context.withCharset(charsetName);
		}
	}

	/**
	 * Returns the {@link MimeType} if present in the {@link MessageContext}.
	 */
	public static MimeType getMimeType(Message message) {
		if(Message.isEmpty(message) || message.getContext().isEmpty()) {
			return null;
		}

		MimeType mimeType = (MimeType)message.getContext().get(MessageContext.METADATA_MIMETYPE);
		if(mimeType == null) {
			LOG.trace("no mimetype found in MessageContext");
			return null;
		}

		if(message.getCharset() != null) { //and is character data?
			LOG.trace("found mimetype [{}] in MessageContext with charset [{}]", ()->mimeType, message::getCharset);
			return new MimeType(mimeType, Charset.forName(message.getCharset()));
		}

		LOG.trace("found mimetype [{}] in MessageContext without charset", mimeType);
		return mimeType;
	}

	public static boolean isMimeType(Message message, MimeType compareTo) {
		MessageContext context = message.getContext();
		MimeType mimeType = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		return mimeType != null && mimeType.includes(compareTo);
	}

	/**
	 * Computes the {@link MimeType} when not available.
	 * <p>
	 * NOTE: This is a resource intensive operation, the first 64k is being read and stored in memory.
	 */
	public static MimeType computeMimeType(Message message) {
		return computeMimeType(message, null);
	}

	/**
	 * Computes the {@link MimeType} when not available, attempts to resolve the Charset when of type TEXT.
	 * <p>
	 * NOTE: This is a resource intensive operation, the first kilobytes of the message are potentially being read and stored in memory.
	 */
	public static MimeType computeMimeType(Message message, String filename) {
		if(Message.isEmpty(message)) {
			return null;
		}

		MessageContext context = message.getContext();
		MimeType contextMimeType = getMimeType(message);
		if(contextMimeType != null) {
			LOG.debug("returning predetermined mimetype [{}]", contextMimeType);
			return contextMimeType;
		}

		String name = (String) context.get(MessageContext.METADATA_NAME);
		if(StringUtils.isNotEmpty(filename)) {
			LOG.trace("using filename from MessageContext [{}]", name);
			name = filename;
		}

		try {
			String mediaType = TIKA.detect(message.asInputStream(), name);
			MimeType mimeType = MimeType.valueOf(mediaType);
			context.withMimeType(mimeType);
			if("text".equals(mimeType.getType()) || message.getCharset() != null) { // is of type 'text' or message has charset
				Charset charset = computeDecodingCharset(message);
				if(charset != null) {
					LOG.debug("found mimetype [{}] with charset [{}]", mimeType, charset);
					return new MimeType(mimeType, charset);
				}
			}

			LOG.debug("found mimetype [{}]", mimeType);
			return mimeType;
		} catch (Exception t) {
			LOG.warn("error parsing message to determine mimetype", t);
			return null;
		}
	}

	/**
	 * Resource intensive operation, preserves the message and calculates an MD5 hash over the entire message.
	 */
	@SuppressWarnings("java:S4790") // MD5 usage is allowed for checksums
	public static String generateMD5Hash(Message message) {
		try {
			if(!message.isRepeatable()) {
				message.preserve();
			}

			try (InputStream inputStream = message.asInputStream()) {
				return DigestUtils.md5DigestAsHex(inputStream);
			}
		} catch (IllegalStateException | IOException e) {
			LOG.warn("unable to read Message or write the MD5 hash", e);
			return null;
		}
	}

	/**
	 * Resource intensive operation, preserves the message and calculates an CRC32 checksum over the entire message.
	 */
	public static Long generateCRC32(Message message) {
		try {
			if(!message.isRepeatable()) {
				message.preserve();
			}

			CRC32 checksum = new CRC32();
			try (InputStream inputStream = new CheckedInputStream(message.asInputStream(), checksum)) {
				long size = IOUtils.consume(inputStream);
				message.getContext().withSize(size);
			}
			return checksum.getValue();
		} catch (IOException e) {
			LOG.warn("unable to read Message", e);
			return null;
		}
	}

	/**
	 * Resource intensive operation, calculates the binary size of a Message.
	 */
	public static long computeSize(Message message) {
		try {
			long size = message.size();
			if(size > Message.MESSAGE_SIZE_UNKNOWN) {
				return size;
			}

			if(!message.isRepeatable()) {
				message.preserve();
			}

			// Preserving the message might make reading the size known. If so, there is no need to compute it.
			size = message.size();
			if(size > Message.MESSAGE_SIZE_UNKNOWN) {
				return size;
			}

			try (InputStream inputStream = message.asInputStream()) {
				long computedSize = IOUtils.consume(inputStream);
				message.getContext().withSize(computedSize);
				return computedSize;
			}
		} catch (IOException e) {
			LOG.warn("unable to read Message", e);
			return Message.MESSAGE_SIZE_UNKNOWN;
		}
	}

	/**
	 * Convert an object to a string. Does not close object when it is of type Message or MessageWrapper.
	 */
	@Deprecated
	public static String asString(Object object) throws IOException {
		if (object == null) {
			return null;
		}
		if (object instanceof String string) {
			return string;
		}
		if (object instanceof Message message) {
			message.assertNotClosed();
			return message.asString();
		}
		if (object instanceof MessageWrapper<?> wrapper) {
			return wrapper.getMessage().asString();
		}
		// In other cases, message can be closed directly after converting to String.
		try (Message message = Message.asMessage(object)) {
			return message.asString();
		}
	}
}
