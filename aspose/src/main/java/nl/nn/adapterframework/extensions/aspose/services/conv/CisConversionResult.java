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
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;
import org.apache.cxf.common.util.StringUtils;
import org.springframework.http.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.util.XmlBuilder;
/**
 * @author
 * 	Gerard van der Hoorn
 */
public class CisConversionResult {

	private static final String PASSWORD_MESSAGE = "Omzetten naar PDF mislukt. Reden: bestand is beveiligd met een wachtwoord!";

	private @Getter @Setter ConversionOption conversionOption;
	private @Getter @Setter MediaType mediaType;
	private @Getter @Setter String documentName;
	private @Getter @Setter String failureReason;
	private @Getter @Setter int numberOfPages;

	private @Getter ByteArrayOutputStream conversionResultHandle = new ByteArrayOutputStream();
	public ByteArrayInputStream getConversionResult(){
		return new ByteArrayInputStream(conversionResultHandle.toByteArray());
	}

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


	public static CisConversionResult createCisConversionResult(ConversionOption conversionOption, MediaType mediaType,
			String documentName, File pdfResultFile, String failureReason, List<CisConversionResult> argAttachments) {

		CisConversionResult cisConversionResult = new CisConversionResult();
		cisConversionResult.setConversionOption(conversionOption);
		cisConversionResult.setMediaType(mediaType);
		cisConversionResult.setDocumentName(documentName);
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


	public List<CisConversionResult> getAttachments() {
		return Collections.unmodifiableList(attachments);
	}

	public void addAttachment(CisConversionResult attachment) {
		this.attachments.add(attachment);
	}

	public boolean isConversionSuccessfull() {
		return failureReason == null;
	}

	public void clear(){
		conversionResultHandle = new ByteArrayOutputStream();
	}
	@Override
	public final String toString() { //HIER
		StringBuilder builder = new StringBuilder(super.toString());
		builder.append(String.format("ConversionOption=[%s]", getConversionOption()));
		builder.append(String.format("mediaType=[%s]", getMediaType()));
		builder.append(String.format("documentName=[%s]", getDocumentName()));
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
			if(StringUtils.isEmpty(getFailureReason())){
				main.addAttribute("sessionKey", this.getSessionKeyName());
			}
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
				if(StringUtils.isEmpty(attachment.getFailureReason())){
					attachmentAsXml.addAttribute("sessionKey", attachment.getSessionKeyName());
				}
				attachmentsAsXml.addSubElement(attachmentAsXml);

				buildXmlFromResult(attachmentAsXml, attachment, false);
			}
			main.addSubElement(attachmentsAsXml);
		}
	}

	private @Getter @Setter String sessionKeyName = "Converted.Document.";
	public void populateSession(PipeLineSession session){
		populateSession(this, session, 0);
	}

	private void populateSession(CisConversionResult result, PipeLineSession session, int index){
		result.setSessionKeyName(result.getSessionKeyName()+index);
		session.put(result.getSessionKeyName(), new Message(result.getConversionResult()));
		List<CisConversionResult> attachmentList = result.getAttachments();
		if (attachmentList != null && !attachmentList.isEmpty()) {
			for (int i = 0; i < attachmentList.size(); i++) {
				index++;
				populateSession(attachmentList.get(i), session, index);
			}
		}
	}
}
