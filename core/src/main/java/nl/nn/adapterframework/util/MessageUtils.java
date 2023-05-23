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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.AttachmentPart;
import javax.xml.soap.MimeHeader;
import javax.xml.soap.SOAPException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.springframework.util.DigestUtils;
import org.springframework.util.MimeType;

import com.ibm.icu.text.CharsetDetector;
import com.ibm.icu.text.CharsetMatch;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;

public abstract class MessageUtils {
	private static final Logger LOG = LogUtil.getLogger(MessageUtils.class);
	private static int charsetConfidenceLevel = AppConstants.getInstance().getInt("charset.confidenceLevel", 65);

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
			result.put(MessageContext.HEADER_PREFIX + name, request.getHeader(name));
		}

		return result;
	}

	public static MessageContext getContext(Iterator<MimeHeader> mimeHeaders) {
		MessageContext result = new MessageContext();
		while (mimeHeaders.hasNext()) {
			MimeHeader header = mimeHeaders.next();
			String name = header.getName();
			if("Content-Transfer-Encoding".equals(name)) {
				try {
					Charset charset = Charset.forName(header.getValue());
					result.withCharset(charset);
				} catch (Exception e) {
					LOG.warn("Could not determine charset", e);
				}
			} else if("Content-Type".equals(name)) {
				result.withMimeType(header.getValue());
			} else {
				result.put(MessageContext.HEADER_PREFIX + name, header.getValue());
			}
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
			// We want the context because of the request headers
			return Message.nullMessage(getContext(request));
		}
	}

	@SuppressWarnings("unchecked")
	public static Message parse(AttachmentPart soapAttachment) throws SOAPException {
		return new Message(soapAttachment.getRawContentBytes(), getContext(soapAttachment.getAllMimeHeaders()));
	}

	/**
	 * Reads the first 10k bytes of (binary) messages to determine the charset when not present in the {@link MessageContext}.
	 * @throws IOException when it cannot read the first 10k bytes.
	 */
	public static Charset computeDecodingCharset(Message message) throws IOException {
		return computeDecodingCharset(message, charsetConfidenceLevel);
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
		detector.setText(message.getMagic());
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
			Map<String, Object> context = message.getContext();
			if(context != null) {
				context.put(MessageContext.METADATA_CHARSET, charsetName);
			}
		}
	}

	/**
	 * Returns the {@link MimeType} if present in the {@link MessageContext}.
	 */
	public static MimeType getMimeType(Message message) {
		if(Message.isEmpty(message) || message.getContext() == null) {
			return null;
		}

		MimeType mimeType = (MimeType)message.getContext().get(MessageContext.METADATA_MIMETYPE);
		if(mimeType == null) {
			return null;
		}

		if(message.getCharset() != null) { //and is character data?
			return new MimeType(mimeType, Charset.forName(message.getCharset()));
		}

		return mimeType;
	}

	public static boolean isMimeType(Message message, MimeType compareTo) {
		Map<String, Object> context = message.getContext();
		MimeType mimeType = (MimeType) context.get(MessageContext.METADATA_MIMETYPE);
		return (mimeType != null && mimeType.includes(compareTo));
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
	 * NOTE: This is a resource intensive operation, the first 64k is being read and stored in memory.
	 */
	public static MimeType computeMimeType(Message message, String filename) {
		if(Message.isEmpty(message)) {
			return null;
		}

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
			TikaConfig tika = new TikaConfig();
			Metadata metadata = new Metadata();
			metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY, name);
			int tikaMimeMagicLength = tika.getMimeRepository().getMinLength();
			byte[] magic = message.getMagic(tikaMimeMagicLength);
			if(magic.length == 0) {
				return null;
			}
			org.apache.tika.mime.MediaType tikaMediaType = tika.getDetector().detect(new ByteArrayInputStream(magic), metadata);
			mimeType = MimeType.valueOf(tikaMediaType.toString());
			context.put(MessageContext.METADATA_MIMETYPE, mimeType);
			if("text".equals(mimeType.getType()) || message.getCharset() != null) { // is of type 'text' or message has charset
				Charset charset = computeDecodingCharset(message);
				if(charset != null) {
					return new MimeType(mimeType, charset);
				}
			}
			return mimeType;
		} catch (Exception t) {
			LOG.warn("error parsing message to determine mimetype", t);
		}

		LOG.info("unable to determine mimetype");
		return null;
	}

	/**
	 * Resource intensive operation, preserves the message and calculates an MD5 hash over the entire message.
	 */
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
		}
		return null;
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
				message.getContext().put(MessageContext.METADATA_SIZE, size);
			}
			return checksum.getValue();
		} catch (IOException e) {
			LOG.warn("unable to read Message", e);
		}
		return null;
	}

	/**
	 * Resource intensive operation, calculates the binary size of a Message.
	 */
	public static long calculateSize(Message message) {
		try {
			long size = message.size();
			if(size > Message.MESSAGE_SIZE_UNKNOWN) {
				return size;
			}

			if(!message.isRepeatable()) {
				message.preserve();
			}

			try (InputStream inputStream = message.asInputStream()) {
				long computedSize = IOUtils.consume(inputStream);
				message.getContext().put(MessageContext.METADATA_SIZE, computedSize);
				return computedSize;
			}
		} catch (IOException e) {
			LOG.warn("unable to read Message", e);
		}
		return -1;
	}
}
