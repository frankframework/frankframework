/**
 * 
 */
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.axis.utils.ByteArrayOutputStream;
import org.apache.log4j.Logger;

import com.aspose.pdf.Document;
import com.aspose.pdf.FileSpecification;
import com.aspose.pdf.PageMode;
import com.aspose.pdf.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertorUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.FileConstants;
import nl.nn.adapterframework.extensions.aspose.services.util.StringsUtil;

/**
 * This class will combine seperate pdf files to a single pdf with attachments.
 * None existing files in a CisConversionResult will be skipped!
 * 
 * TODO: Refactor this class
 */
public class PdfAttachmentUtil {

	private static final Logger LOGGER = Logger.getLogger(PdfAttachmentUtil.class);

	private List<CisConversionResult> cisConversionResultList;

	private File rootPdf;

	private Document pdfDocument;

	/**
	 * Private constructor om te voorkomen dat deze klasse (met static methoden)
	 * aangemaakt kan worden.
	 */
	private PdfAttachmentUtil(List<CisConversionResult> cisConversionResultList, File file) {
		this.cisConversionResultList = cisConversionResultList;
		this.rootPdf = file;
	}

	public PdfAttachmentUtil(CisConversionResult result) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Adds the given files in cisConversionResultList to the given rootPdf file.
	 * 
	 * @param cisConversionResultList
	 * @param rootPdf
	 * @throws IOException
	 */
	static void addAttachmentInSinglePdf(List<CisConversionResult> cisConversionResultList, File rootPdf)
			throws IOException {
		PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(cisConversionResultList, rootPdf);
		try {
			pdfAttachmentUtil.addAttachmentInSinglePdf();
		} finally {
			// TODO: fix this part
			pdfAttachmentUtil.finit(null);
		}
	}

	protected void addAttachmentToPdf(CisConversionResult result, InputStream fileToAttach, String filename, String extension)
			throws IOException {
		PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(null, result.getPdfResultFile());
		try (BufferedInputStream attachmentDocumentStream = new BufferedInputStream(fileToAttach)) {
			pdfAttachmentUtil.addFileToPdf(attachmentDocumentStream, filename, extension, result);
		} finally {
			pdfAttachmentUtil.finit(result);
		}
	}

	/**
	 * Create a new pdf rootPdf based on the pdf in cisConversionResult and add all
	 * files specified in cisConversionResult.attachments to it.
	 * <p>
	 * Note: Nothing is changed to the given cisConversionResult object and its
	 * underlying files.
	 * </p>
	 * 
	 * if there are no attachments null is returned otherwise rootPdf.
	 * 
	 * @param cisConversionResult
	 *            wiht the given pdf and its attachments.
	 * @param rootPdf
	 *            the pdf created with the attachments embedded.
	 * @throws IOException
	 */
	private void addAttachmentInSinglePdf() throws IOException {

		for (CisConversionResult cisConversionResultAttachment : cisConversionResultList) {

			if (cisConversionResultAttachment.getPdfResultFile() != null) {

				try (InputStream attachmentDocumentStream = new BufferedInputStream(
						new FileInputStream(cisConversionResultAttachment.getPdfResultFile()))) {

					addFileToPdf(attachmentDocumentStream, cisConversionResultAttachment.getDocumentName(),
							ConvertorUtil.PDF_FILETYPE, cisConversionResultAttachment);
				}

			} else {
				LOGGER.debug("Skipping file because it is not available.");
			}
		}
	}

	private void finit(CisConversionResult result) {
		if (pdfDocument != null) {

			pdfDocument.save();
			pdfDocument.freeMemory();
			pdfDocument.dispose();
			pdfDocument.close();

			pdfDocument = null;
		}
	}

	private void addFileToPdf(InputStream attachmentDocumentStream, String fileName, String extension,
			CisConversionResult result) {
		// Determine the document name to use. (Convert any invalid name to a valid
		// filename.
		String documentName = ConvertorUtil.createTidyFilename(convertToValidFileName(fileName), extension);

		LOGGER.debug("Adding attachment with document name \"" + documentName + "\" (original: \"" + fileName + "\")");

		// Add an attachment to document's attachment collection
		getPdfDocument(result.getResultFilePath()).getEmbeddedFiles()
				.add(new FileSpecification(attachmentDocumentStream, documentName));
	}

	private String convertToValidFileName(String value) {
		String result = value;
		if (!StringsUtil.isBlank(value)) {
			result = value.replaceAll(FileConstants.REPLACE_CHARACTERS_IN_NAME_REGEX, FileConstants.REPLACE_CHARACTER);
			if (!result.equals(value)) {
				LOGGER.debug("Updated filename to a valid filename from \"" + value + "\" to \"" + result + "\"");
			}
		}
		return result;
	}

	private Document getPdfDocument(String filePath) {

		if (pdfDocument == null) {

			// Open the base pdf.
			pdfDocument = new Document(filePath);

			// UseAttachments means "Optional attachments panel set to visible" used so that
			// the attachments are shown.
			pdfDocument.setPageMode(PageMode.UseAttachments);
		}

		return pdfDocument;
	}

	public static InputStream combineFiles(InputStream parent, InputStream attachment, String fileNameToAttach) {
		Document pdfDoc = new Document(parent);
		pdfDoc.setPageMode(PageMode.UseAttachments);

		pdfDoc.getEmbeddedFiles().add(new FileSpecification(attachment, fileNameToAttach));

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		pdfDoc.save(baos, SaveFormat.Pdf);
		return new ByteArrayInputStream(baos.toByteArray());
	}

}
