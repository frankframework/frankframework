/*
   Copyright 2019, 2021-2026 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;

import com.aspose.pdf.Document;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.extensions.aspose.ConversionOption;
import org.frankframework.stream.Message;
import org.frankframework.stream.PathMessage;
import org.frankframework.util.XmlBuilder;

public class CisConversionResult {

	private static final String DEFAULT_FILENAME = "default_filename";
	private static final String PASSWORD_MESSAGE = "Failed to convert to PDF. Reason: The file has been protected with a password.";

	private @Getter @Setter ConversionOption conversionOption;
	private @Getter @Setter MediaType mediaType;
	private @Getter String documentName;
	private @Getter @Setter String failureReason;
	private @Getter @Setter int numberOfPages;
	private Message message;
	private @Setter String resultSessionKey;

	/**
	 * List with documents which where part of the source document (e.g. attachments
	 * in mails). Will be an empty list if there are no attachments.
	 * <p>
	 * Note: the attachments will not contain a pdfResultFile because that will be
	 * in overall pdf file (flat pdf file) for SINGLEPDF conversions otherwise it
	 * can contain pdf files.
	 * </p>
	 */
	private final List<Message> attachments = new ArrayList<>();

	/**
	 * If set, the converted PDF file should be placed in this location.
	 */
	private Path resultFileLocation;

	public void setPersistToDisk(String pdfOutputLocation) throws IOException {
		Path resultFileDirectory = Paths.get(pdfOutputLocation);
		resultFileLocation = Files.createTempFile(resultFileDirectory, "msg", ".pdf");
	}

	public void setDocumentName(String filename) {
		documentName = StringUtils.defaultIfBlank(filename, DEFAULT_FILENAME);
	}

	public void setMessage(Message message) throws IOException {
		this.message = message;
		if (message == null) return;

		if (!message.getContext().containsKey("Pdf.Pages")) {
			try (InputStream inStream = message.asInputStream()) {
				try(Document doc = new Document(inStream)) {
					numberOfPages = doc.getPages().size();
				}
			}
		} else {
			numberOfPages = (int) message.getContext().get("Pdf.Pages");
		}
	}

	public Message getMessage() throws IOException {
		if (resultFileLocation == null) {
			return message;
		}

		try (InputStream in = message.asInputStream(); OutputStream out = Files.newOutputStream(resultFileLocation)) {
			in.transferTo(out);
		}
		return PathMessage.asTemporaryMessage(resultFileLocation);
	}

	public Message rawMessage() {
		return message;
	}

	public static CisConversionResult createPasswordFailureResult(String filename, ConversionOption conversionOption, MediaType mediaTypeReceived) {
		StringBuilder msg = new StringBuilder();
		if (filename != null) {
			msg.append(filename);
		}
		msg.append(" ").append(PASSWORD_MESSAGE);
		return createFailureResult(conversionOption, mediaTypeReceived, filename, msg.toString());
	}

	public static CisConversionResult createFailureResult(ConversionOption conversionOption,
			MediaType mediaTypeReceived, String documentName, String failureReason) {

		CisConversionResult cisConversionResult = new CisConversionResult();
		cisConversionResult.setConversionOption(conversionOption);
		cisConversionResult.setMediaType(mediaTypeReceived);
		cisConversionResult.setDocumentName(documentName);
		cisConversionResult.setFailureReason(failureReason);

		return cisConversionResult;
	}

	@NonNull
	public List<Message> getAttachments() {
		return Collections.unmodifiableList(attachments);
	}

	public void addAttachment(Message attachment) {
		this.attachments.add(attachment);
	}

	public boolean isConversionSuccessful() {
		return failureReason == null;
	}

	@Override
	public final String toString() {
		return super.toString() + String.format("ConversionOption=[%s]", getConversionOption()) +
				String.format("mediaType=[%s]", getMediaType()) +
				String.format("documentName=[%s]", getDocumentName()) +
				"pdfResultFile=[%s]".formatted(resultFileLocation == null ? "null" : resultFileLocation) +
				String.format("sessionKey=[%s]", resultSessionKey) +
				String.format("failureReason=[%s]", getFailureReason()) +
				"attachments=[%s]".formatted(getAttachments());
	}

	/**
	 * Creates an XML containing conversion results both attachments and the main document.
	 */
	public XmlBuilder toXML() throws IOException {
		return toXML(new XmlBuilder("main"));
	}

	/**
	 * Append this result to the parent
	 */
	public XmlBuilder toXML(XmlBuilder xmlResult) throws IOException {
		xmlResult.addAttribute("conversionOption", getConversionOption().getValue());
		xmlResult.addAttribute("mediaType", getMediaType().toString());
		xmlResult.addAttribute("documentName", getDocumentName());
		xmlResult.addAttribute("failureReason", getFailureReason());
		xmlResult.addAttribute("numberOfPages", getNumberOfPages());

		if (message != null && resultFileLocation != null) {
			xmlResult.addAttribute("convertedDocument", resultFileLocation.toString());
		}
		xmlResult.addAttribute("sessionKey", resultSessionKey);
		return xmlResult;
	}

}
