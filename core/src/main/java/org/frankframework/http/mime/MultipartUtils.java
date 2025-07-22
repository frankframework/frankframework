/*
   Copyright 2021-2025 WeAreFrank!

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
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.annotation.Nullable;
import jakarta.mail.BodyPart;
import jakarta.mail.Header;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.MimeMultipart;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.SOAPException;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.http.InputStreamDataSource;
import org.frankframework.http.PartMessage;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.MessageUtils;
import org.frankframework.util.XmlBuilder;

@Log4j2
public class MultipartUtils {

	private MultipartUtils() {
		throw new IllegalStateException("Don't construct utility class");
	}

	public static final String FORM_DATA = "form-data";
	public static final String MULTIPART = "multipart/";
	public static final String ATTACHMENT = "attachment";
	public static final String METADATA_PARTNAME = "Metadata.PartName";
	public static final String MULTIPART_ATTACHMENTS_SESSION_KEY   = "multipartAttachments";

	public static boolean isMultipart(HttpServletRequest request) {
		String httpMethod = request.getMethod().toUpperCase();
		if("POST".equals(httpMethod) || "PUT".equals(httpMethod) || "PATCH".equals(httpMethod)) {
			return request.getContentType() != null && request.getContentType().startsWith(MULTIPART);
		}
		return false;
	}

	@Nullable
	public static String getFieldName(Part part) {
		try {
			String[] id = part.getHeader("Content-ID"); // MTOM requests
			if(id != null && StringUtils.isNotBlank(id[0])) {
				String idField = id[0];
				return idField.substring(1, idField.length()-1);
			}

			String[] cd = part.getHeader("Content-Disposition"); // MTOM Attachments and FORM-DATA requests
			if(cd != null) {
				String cdFields = cd[0]; // form-data; name="file1"; filename="file1" || attachment; name="file1"; filename="file1"
				if(cdFields != null) {
					return parseParameterField(cdFields, "name");
				}
			}
		} catch (MessagingException e) {
			log.warn("unable to determine fieldname from part [{}]", part, e);
		}
		return null;
	}

	@Nullable
	public static String getFieldName(AttachmentPart part) {
		String id = part.getContentId(); // MTOM requests
		if(StringUtils.isNotBlank(id)) {
			return id.substring(1, id.length()-1); // Strip off < and > chars
		}

		String[] cd = part.getMimeHeader("Content-Disposition"); // MTOM Attachments and FORM-DATA requests
		if(cd != null) {
			String cdFields = cd[0]; // form-data; name="file1"; filename="file1" || attachment; name="file1"; filename="file1"
			if(cdFields != null) {
				return parseParameterField(cdFields, "name");
			}
		}
		return null;
	}

	@Nullable
	public static String getFileName(Part part) throws MessagingException {
		String[] cd = part.getHeader("Content-Disposition");
		return getFileName(cd);
	}

	@Nullable
	public static String getFileName(AttachmentPart attachmentPart) {
		String[] cd = attachmentPart.getMimeHeader("Content-Disposition");
		return getFileName(cd);
	}

	/**
	 * Check for the filename in the <code>Content-Disposition</code> header.
	 * Eg. Content-Disposition form-data; name="file"; filename="dummy.jpg"
	 * Eg. Content-Disposition attachment; filename="dummy.jpg"
	 */
	@Nullable
	private static String getFileName(String[] contentDispositionHeader) {
		if(contentDispositionHeader != null && contentDispositionHeader.length > 0) {
			String cdFields = contentDispositionHeader[0];
			if (cdFields.startsWith(FORM_DATA) || cdFields.startsWith(ATTACHMENT)) {
				String filename = parseParameterField(cdFields, "filename");
				if(StringUtils.isNotBlank(filename)) {
					return filename.trim();
				}
			}
		}
		return null;
	}

	public static MessageContext getContext(Part part) throws MessagingException {
		MessageContext result = new MessageContext();
		result.withMimeType(part.getContentType());
		result.withSize(part.getSize());
		String filename = getFileName(part);
		if (StringUtils.isNotBlank(filename)) {
			result.withName(filename);
		}

		String id = getFieldName(part);
		if(id != null) {
			result.put(METADATA_PARTNAME, getFieldName(part));
		}

		Enumeration<Header> names = part.getAllHeaders();
		while(names.hasMoreElements()) {
			Header header = names.nextElement();
			result.put(MessageContext.HEADER_PREFIX + header.getName(), header.getValue());
		}

		return result;
	}

	public static boolean isBinary(Part part) {
		try {
			// Check if a filename is present (indicating it's a file and not a field)
			String filename = getFileName(part);
			if(filename != null) {
				return true;
			}

			// Check if the transfer encoding has been set when MTOM
			String[] cte = part.getHeader("Content-Transfer-Encoding");
			if(cte != null) {
				String cteFields = cte[0]; // Content-Transfer-Encoding - binary || 8bit
				if("binary".equalsIgnoreCase(cteFields)) {
					return true;
				}
			}
		} catch (MessagingException e) {
			log.warn("unable to determine if part [{}] is binary", part, e);
		}
		return false;
	}

	private static boolean isBinary(AttachmentPart part) {
		// Check if a filename is present (indicating it's a file and not a field)
		String filename = getFileName(part);
		if(filename != null) {
			return true;
		}

		// Check if the transfer encoding has been set when MTOM
		String[] cte = part.getMimeHeader("Content-Transfer-Encoding");
		if(cte != null) {
			String cteFields = cte[0]; // Content-Transfer-Encoding - binary || 8bit
			if("binary".equalsIgnoreCase(cteFields)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private static String parseParameterField(String cdFields, String fieldName) {
		for(String field : cdFields.split(";")) {
			String[] f = field.trim().split("=", 2);
			String name = f[0];
			if(f.length > 1 && fieldName.equalsIgnoreCase(name) && StringUtils.isNotBlank(f[1])) {
				return f[1].substring(1, f[1].length()-1);
			}
		}
		return null;
	}

	public record MultipartMessages(Message multipartXml, Map<String, Message> messages) {}

	public static MultipartMessages parseMultipart(InputStream inputStream, String contentType) throws IOException {
		try (InputStream ignored = inputStream) {
			final InputStreamDataSource dataSource = new InputStreamDataSource(contentType, inputStream); // The entire InputStream will be read here!
			final MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
			final XmlBuilder attachments = new XmlBuilder("parts");
			final Map<String, Message> parts = new LinkedHashMap<>();

			for (int i = 0; i < mimeMultipart.getCount(); i++) {
				final BodyPart bodyPart = mimeMultipart.getBodyPart(i);
				final String fieldName = getFieldName(bodyPart);
				if(StringUtils.isEmpty(fieldName)) {
					log.info("unable to determine fieldname skipping part");
					continue;
				}

				final XmlBuilder attachment = new XmlBuilder("part");
				attachment.addAttribute("name", fieldName);
				PartMessage message = new PartMessage(bodyPart);
				parts.put(fieldName, message);
				if (!isBinary(bodyPart)) {
					// Process regular form field (input type="text|radio|checkbox|etc", select, etc).
					log.trace("setting multipart formField [{}] to [{}]", fieldName, message);
					attachment.addAttribute("type", "text");
					attachment.addAttribute("value", message.asString());
				} else {
					// Process form file field (input type="file").
					final String fileName = getFileName(bodyPart);
					log.trace("setting parameter [{}] to input stream of file [{}]", fieldName, fileName);

					attachment.addAttribute("type", "file");
					attachment.addAttribute("filename", fileName);
					attachment.addAttribute("size", message.size());
					attachment.addAttribute("sessionKey", fieldName);
					attachment.addAttribute("mimeType", extractMimeType(bodyPart.getContentType()));
				}
				attachments.addSubElement(attachment);
			}

			return new MultipartMessages(attachments.asMessage(), parts);
		} catch(MessagingException e) {
			throw new IOException("could not read mime multipart request", e);
		}
	}

	public static MultipartMessages parseMultipart(Iterator<AttachmentPart> attachmentParts) {
		final XmlBuilder attachments = new XmlBuilder("parts");
		final Map<String, Message> parts = new LinkedHashMap<>();
		int i = 1;
		while (attachmentParts.hasNext()) {
			try {
				AttachmentPart attachmentPart = attachmentParts.next();
				final String fieldName = getFieldName(attachmentPart);
				if(StringUtils.isEmpty(fieldName)) {
					log.info("unable to determine fieldname skipping part");
					continue;
				}

				// may be duplicate so we cannot use the fieldname
				String partName = "attachment" + (i++);

				Message message = new Message(attachmentPart.getRawContentBytes(), MessageUtils.getContext(attachmentPart.getAllMimeHeaders()));

				XmlBuilder attachment = new XmlBuilder("part");
				parts.put(partName, message);
				attachment.addAttribute("name", fieldName);
				attachment.addAttribute("type", isBinary(attachmentPart) ? "file" : "text");
				attachment.addAttribute("filename", getFileName(attachmentPart));
				attachment.addAttribute("size", message.size());
				attachment.addAttribute("sessionKey", partName);
				attachment.addAttribute("mimeType", extractMimeType(attachmentPart.getContentType()));
				attachments.addSubElement(attachment);

			} catch (SOAPException e) {
				log.warn("Could not store attachment in session key", e);
			}
		}

		return new MultipartMessages(attachments.asMessage(), parts);
	}

	private static String extractMimeType(String contentType) {
		final int semicolon = contentType.indexOf(";");
		if(semicolon >= 0) {
			return contentType.substring(0, semicolon);
		} else {
			return contentType;
		}
	}
}
