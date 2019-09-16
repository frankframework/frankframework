/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.tika.mime.MediaType;

import com.google.common.base.MoreObjects;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;

/**
 * @author <a href="mailto:gerard_van_der_hoorn@deltalloyd.nl">Gerard van der
 *         Hoorn</a> (d937275)
 */
public class CisConversionResult {

	private final static String PASSWORD_MESSAGE = "Omzetten naar PDF mislukt. Reden: bestand is beveiligd met een wachtwoord!";

	private ConversionOption conversionOption;
	private MediaType mediaType;
	private String documentName;
	private String failureReason;
	private InputStream fileStream;

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

	/**
	 * CreateCisConversionResult
	 * 
	 * @param conversionOption
	 * @param mediaType
	 * @param documentName
	 * @param pdfResultFile
	 * @param failureReason
	 * @param argAttachments
	 * @return
	 */
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
	 *
	 * @param conversionOption
	 * @param mediaTypeReceived
	 * @param documentNameOriginal
	 * @param pdfResultFile
	 * @param attachments
	 * @return
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

	public InputStream getFileStream() {
		return fileStream;
	}

	public void setFileStream(InputStream fileStream) {
		this.fileStream = fileStream;
	}

	@Override
	public final String toString() {
		return MoreObjects.toStringHelper(this).add("ConversionOption", getConversionOption())
				.add("mediaType", getMediaType()).add("documentName", getDocumentName())
				.add("pdfResultFile", getPdfResultFile() == null ? "null" : getPdfResultFile().getName())
				.add("failureReason", getFailureReason()).add("attachments", getAttachments()).toString();
	}
}