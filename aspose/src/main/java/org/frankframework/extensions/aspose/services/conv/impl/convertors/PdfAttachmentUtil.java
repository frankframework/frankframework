/*
   Copyright 2019-2021 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv.impl.convertors;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import com.aspose.pdf.Document;
import com.aspose.pdf.FileSpecification;
import com.aspose.pdf.PageMode;
import com.aspose.pdf.SaveFormat;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.extensions.aspose.services.util.ConvertorUtil;
import org.frankframework.extensions.aspose.services.util.FileConstants;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;

/**
 * This class will combine seperate pdf files to a single pdf with attachments.
 * None existing files in a CisConversionResult will be skipped!
 *
 */
@Log4j2
public class PdfAttachmentUtil {
	private List<CisConversionResult> cisConversionResultList;

	private final File rootPdf;

	private Document pdfDocument;

	public PdfAttachmentUtil(CisConversionResult cisConversionResultAttachment, File rootFile) {
		this.cisConversionResultList = new ArrayList<>();
		this.cisConversionResultList.add(cisConversionResultAttachment);
		this.rootPdf = rootFile;
	}

	public PdfAttachmentUtil(File pdfResultFile) {
		this.rootPdf = pdfResultFile;
	}

	protected void addAttachmentToPdf(Message fileToAttach, String filename, String extension) throws IOException {
		try (InputStream attachmentDocumentStream = fileToAttach.asInputStream()) {
			addFileToPdf(attachmentDocumentStream, filename, extension);
		} finally {
			finish();
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
	 */
	protected void addAttachmentInSinglePdf() throws IOException {
		try {
			for (CisConversionResult cisConversionResultAttachment : cisConversionResultList) {
				if (cisConversionResultAttachment.getPdfResultFile() != null) {
					try (InputStream attachmentDocumentStream = new BufferedInputStream(Files.newInputStream(cisConversionResultAttachment.getPdfResultFile().toPath()))) {
						addFileToPdf(attachmentDocumentStream, cisConversionResultAttachment.getDocumentName(), ConvertorUtil.PDF_FILETYPE);
					}
				} else {
					log.debug("skipping file because it is not available.");
				}
			}
		} finally {
			finish();
		}
	}

	private void finish() {
		if (pdfDocument != null) {

			pdfDocument.save();
			pdfDocument.freeMemory();
			pdfDocument.close();

			pdfDocument = null;
		}
	}

	private void addFileToPdf(InputStream attachmentDocumentStream, String fileName, String extension) {
		// Determine the document name to use. (Convert any invalid name to a valid
		// filename).
		String documentName = ConvertorUtil.createTidyFilename(convertToValidFileName(fileName), extension);

		log.debug("adding attachment with document name [{}] (original: [{}])", documentName, fileName);

		// Add an attachment to document's attachment collection
		getPdfDocument(rootPdf.getAbsolutePath()).getEmbeddedFiles().add(new FileSpecification(attachmentDocumentStream, documentName));
	}

	@Nullable
	private String convertToValidFileName(@Nullable String value) {
		if (StringUtils.isBlank(value)) {
			return null;
		}
		String result = value.replaceAll(FileConstants.REPLACE_CHARACTERS_IN_NAME_REGEX, FileConstants.REPLACE_CHARACTER);
		if (!result.equals(value)) {
			log.debug("updated filename to a valid filename from [{}] to [{}]", value, result);
		}
		return result;
	}

	@Nonnull
	private Document getPdfDocument(@Nonnull String filePath) {
		if (pdfDocument == null) {
			// Open the base pdf.
			pdfDocument = new Document(filePath);

			// UseAttachments means "Optional attachments panel set to visible" used so that
			// the attachments are shown.
			pdfDocument.setPageMode(PageMode.UseAttachments);
		}

		return pdfDocument;
	}

	@Nonnull
	public static Message combineFiles(@Nonnull Message parent, @Nonnull Message attachment, String fileNameToAttach, String charset) throws IOException {
		try (Document pdfDoc = new Document(parent.asInputStream(charset))) {
			pdfDoc.setPageMode(PageMode.UseAttachments);

			pdfDoc.getEmbeddedFiles().add(new FileSpecification(attachment.asInputStream(charset), fileNameToAttach));
			MessageBuilder messageBuilder = new MessageBuilder();
			try (OutputStream out = messageBuilder.asOutputStream()) {
				pdfDoc.save(out, SaveFormat.Pdf);
			}

			return messageBuilder.build();
		}
	}

}
