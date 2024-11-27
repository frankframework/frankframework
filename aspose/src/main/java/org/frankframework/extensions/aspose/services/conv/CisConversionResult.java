/*
   Copyright 2019, 2021-2022 WeAreFrank!

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;

import org.springframework.http.MediaType;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.extensions.aspose.ConversionOption;
import org.frankframework.util.XmlBuilder;

/**
 * @author
 * 	Gerard van der Hoorn
 */
public class CisConversionResult {

	private static final String PASSWORD_MESSAGE = "Failed to convert to PDF. Reason: The file has been protected with a password.";

	private @Getter @Setter ConversionOption conversionOption;
	private @Getter @Setter MediaType mediaType;
	private @Getter @Setter String documentName;
	private @Getter @Setter String failureReason;
	private @Getter @Setter int numberOfPages;
	private @Getter @Setter String resultFilePath;
	private @Getter @Setter String resultSessionKey;

	/**
	 * List with documents which where part of the source document (e.g. attachments
	 * in mails). Will be an empty list if there are no attachments.
	 * <p>
	 * Note: the attachments will not contain a pdfResultFile because that will be
	 * in overall pdf file (flat pdf file) for SINGLEPDF conversions otherwise it
	 * can contain pdf files.
	 * </p>
	 */
	private final List<CisConversionResult> attachments = new ArrayList<>();

	/**
	 * Converted document when succeeded (otherwise <code>null</code>)
	 */
	@Setter @Getter private File pdfResultFile;

	public static CisConversionResult createCisConversionResult(ConversionOption conversionOption, MediaType mediaType,
			String documentName, File pdfResultFile, String failureReason, List<CisConversionResult> argAttachments) {

		CisConversionResult cisConversionResult = new CisConversionResult();
		cisConversionResult.setConversionOption(conversionOption);
		cisConversionResult.setMediaType(mediaType);
		cisConversionResult.setDocumentName(documentName);
		cisConversionResult.setPdfResultFile(pdfResultFile);
		cisConversionResult.setFailureReason(failureReason);
		if (argAttachments != null) {
			for (CisConversionResult attachment : argAttachments) {
				cisConversionResult.addAttachment(attachment);
			}
		}

		return cisConversionResult;
	}

	/**
	 * Create a successful CisConversionResult
	 */
	public static CisConversionResult createSuccessResult(ConversionOption conversionOption,
			MediaType mediaTypeReceived, String documentName, File pdfResultFile,
			List<CisConversionResult> attachments) {
		return createCisConversionResult(conversionOption, mediaTypeReceived, documentName, pdfResultFile, null,
				attachments);
	}

	public static CisConversionResult createFailureResult(ConversionOption conversionOption,
			MediaType mediaTypeReceived, String documentName, String failureReason) {
		return createCisConversionResult(conversionOption, mediaTypeReceived, documentName, null, failureReason, null);
	}

	public static CisConversionResult createFailureResult(ConversionOption conversionOption,
			MediaType mediaTypeReceived, String documentName, String failureReason,
			List<CisConversionResult> attachments) {
		return createCisConversionResult(conversionOption, mediaTypeReceived, documentName, null, failureReason,
				attachments);
	}

	public static CisConversionResult createPasswordFailureResult(String filename, ConversionOption conversionOption,
			MediaType mediaTypeReceived) {
		StringBuilder msg = new StringBuilder();
		if (filename != null) {
			msg.append(filename);
		}
		msg.append(" ").append(PASSWORD_MESSAGE);
		return createFailureResult(conversionOption, mediaTypeReceived, filename, msg.toString(), null);
	}

	@Nonnull
	public List<CisConversionResult> getAttachments() {
		return Collections.unmodifiableList(attachments);
	}

	public void addAttachment(CisConversionResult attachment) {
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
				"pdfResultFile=[%s]".formatted(getPdfResultFile() == null ? "null" : getPdfResultFile().getName()) +
				String.format("sessionKey=[%s]", getResultSessionKey()) +
				String.format("failureReason=[%s]", getFailureReason()) +
				"attachments=[%s]".formatted(getAttachments());
	}

	/**
	 * Creates and xml containing conversion results both attachments and the main document.
	 */
	public void buildXmlFromResult(XmlBuilder main, boolean isRoot) {
		buildXmlFromResult(main, this, isRoot);
	}
	private void buildXmlFromResult(XmlBuilder main, CisConversionResult cisConversionResult, boolean isRoot) {
		if(isRoot) {
			main.addAttribute("conversionOption", this.getConversionOption().getValue());
			main.addAttribute("mediaType", this.getMediaType().toString());
			main.addAttribute("documentName", this.getDocumentName());
			main.addAttribute("failureReason", this.getFailureReason());
			main.addAttribute("numberOfPages", this.getNumberOfPages());
			main.addAttribute("convertedDocument", this.getResultFilePath());
			main.addAttribute("sessionKey", this.getResultSessionKey());
		}
		List<CisConversionResult> attachmentList = cisConversionResult.getAttachments();
		if (!attachmentList.isEmpty()) {
			XmlBuilder attachmentsAsXml = new XmlBuilder("attachments");
			for (CisConversionResult attachment : attachmentList) {
				XmlBuilder attachmentAsXml = new XmlBuilder("attachment");
				attachmentAsXml.addAttribute("conversionOption", attachment.getConversionOption().getValue() + "");
				attachmentAsXml.addAttribute("mediaType", attachment.getMediaType().toString());
				attachmentAsXml.addAttribute("documentName", attachment.getDocumentName());
				attachmentAsXml.addAttribute("failureReason", attachment.getFailureReason());
				attachmentAsXml.addAttribute("numberOfPages", attachment.getNumberOfPages());
				attachmentAsXml.addAttribute("convertedDocument", attachment.getResultFilePath());
				attachmentAsXml.addAttribute("sessionKey", attachment.getResultSessionKey());
				attachmentsAsXml.addSubElement(attachmentAsXml);

				buildXmlFromResult(attachmentAsXml, attachment, false);
			}
			main.addSubElement(attachmentsAsXml);
		}
	}
}
