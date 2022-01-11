/*
   Copyright 2019, 2021 WeAreFrank!

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
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.util.XmlBuilder;
/**
 * @author
 * 	Gerard van der Hoorn
 */
public class CisConversionResult {

	private final static String PASSWORD_MESSAGE = "Omzetten naar PDF mislukt. Reden: bestand is beveiligd met een wachtwoord!";

	private ConversionOption conversionOption;
	private MediaType mediaType;
	private String documentName;
	private String failureReason;
	private int numberOfPages;
	private String resultFilePath;
	/**
	 * List with documents which where part of the source document (e.g. attachments
	 * in mails). Will be an empty list if there are no attachments.
	 * <p>
	 * Note: the attachments will not contain a pdfResultFile because that will be
	 * in overall pdf file (flat pdf file) for SINGLEPDF conversions otherwise it
	 * can contain pdf files.
	 * </p>
	 */
	private List<CisConversionResult> attachments = new ArrayList<>();

	/**
	 * Converted document when succeeded (otherwise <code>null</code>) -
	 */
	private File pdfResultFile;

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
		msg.append(" " + PASSWORD_MESSAGE);
		return createFailureResult(conversionOption, mediaTypeReceived, filename, msg.toString(), null);
	}

	public ConversionOption getConversionOption() {
		return conversionOption;
	}

	public void setConversionOption(ConversionOption conversionOption) {
		this.conversionOption = conversionOption;
	}

	public MediaType getMediaType() {
		return mediaType;
	}

	public void setMediaType(MediaType mediaType) {
		this.mediaType = mediaType;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public File getPdfResultFile() {
		return pdfResultFile;
	}

	public void setPdfResultFile(File pdfResultFile) {
		this.pdfResultFile = pdfResultFile;
	}

	public String getFailureReason() {
		return failureReason;
	}

	public void setFailureReason(String failureReason) {
		this.failureReason = failureReason;
	}

	public List<CisConversionResult> getAttachments() {
		return Collections.unmodifiableList(attachments);
	}

	public void addAttachment(CisConversionResult attachment) {
		this.attachments.add(attachment);
	}

	public boolean isConversionSuccessfull() {
		return failureReason == null;
	}

	public int getNumberOfPages() {
		return numberOfPages;
	}

	public void setNumberOfPages(int numberOfPages) {
		this.numberOfPages = numberOfPages;
	}

	public String getResultFilePath() {
		return resultFilePath;
	}

	public void setResultFilePath(String resultFilePath) {
		this.resultFilePath = resultFilePath;
	}

	@Override
	public final String toString() { //HIER
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append(String.format("ConversionOption=[%s]", getConversionOption()));
		builder.append(String.format("mediaType=[%s]", getMediaType()));
		builder.append(String.format("documentName=[%s]", getDocumentName()));
		builder.append(String.format("pdfResultFile=[%s]", getPdfResultFile() == null ? "null" : getPdfResultFile().getName()));
		builder.append(String.format("failureReason=[%s]", getFailureReason()));
		builder.append(String.format("attachments=[%s]", getAttachments()));

		return builder.toString();
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
		}
		List<CisConversionResult> attachmentList = cisConversionResult.getAttachments();
		if (attachmentList != null && !attachmentList.isEmpty()) {
			XmlBuilder attachmentsAsXml = new XmlBuilder("attachments");
			for (int i = 0; i < attachmentList.size(); i++) {
				CisConversionResult attachment = attachmentList.get(i);

				XmlBuilder attachmentAsXml = new XmlBuilder("attachment");
				attachmentAsXml.addAttribute("conversionOption", attachment.getConversionOption().getValue() + "");
				attachmentAsXml.addAttribute("mediaType", attachment.getMediaType().toString());
				attachmentAsXml.addAttribute("documentName", attachment.getDocumentName());
				attachmentAsXml.addAttribute("failureReason", attachment.getFailureReason());
				attachmentAsXml.addAttribute("numberOfPages", attachment.getNumberOfPages());
				attachmentAsXml.addAttribute("convertedDocument", attachment.getResultFilePath());
				attachmentsAsXml.addSubElement(attachmentAsXml);

				buildXmlFromResult(attachmentAsXml, attachment, false);
			}
			main.addSubElement(attachmentsAsXml);
		}
	}
}