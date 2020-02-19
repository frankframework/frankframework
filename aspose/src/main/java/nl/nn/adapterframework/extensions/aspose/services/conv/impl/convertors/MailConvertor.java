/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.email.Attachment;
import com.aspose.email.AttachmentCollection;
import com.aspose.email.MailAddress;
import com.aspose.email.MailMessage;
import com.aspose.email.MhtFormatOptions;
import com.aspose.email.MhtSaveOptions;
import com.aspose.email.TnefLoadOptions;
import com.aspose.words.Document;
import com.aspose.words.HtmlLoadOptions;
import com.aspose.words.LoadFormat;
import com.aspose.words.Node;
import com.aspose.words.NodeType;
import com.aspose.words.SaveFormat;
import com.aspose.words.Shape;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertorUtil;
import nl.nn.adapterframework.util.LogUtil;

class MailConvertor extends AbstractConvertor {

	private static final Logger LOGGER = LogUtil.getLogger(MailConvertor.class);

	private static final float MaxImageWidthInPoints = PageConvertUtil
			.convertCmToPoints(PageConvertUtil.PAGE_WIDHT_IN_CM - 2 * 1.1f);
	private static final float MaxImageHeightInPoints = PageConvertUtil
			.convertCmToPoints(PageConvertUtil.PAGE_HEIGTH_IN_CM - 2 * 1.1f);
	private final String eMailHeaderDateFormat = "dd-MM-yyyy HH:mm:ss";
	private CisConversionService cisConversionService;

	// contains mapping from MediaType to the LoadOption for the aspose word
	// conversion.
	private static final Map<MediaType, com.aspose.email.LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, com.aspose.email.LoadOptions> map = new HashMap<>();
		map.put(new MediaType("message", "rfc822"), null);
		map.put(new MediaType("message", "rfc822.concept"), null);
		map.put(new MediaType("message", "rfc822.ddcim"), null);
		map.put(new MediaType("application", "vnd.ms-outlook"), null);
		map.put(new MediaType("application", "vnd.ms-tnef"), new TnefLoadOptions());

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	MailConvertor(CisConversionService cisConversionService, String pdfOutputLocation) {
		super(pdfOutputLocation,
				MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet().toArray(new MediaType[MEDIA_TYPE_LOAD_FORMAT_MAPPING.size()]));

		this.cisConversionService = cisConversionService;
	}

	@Override
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {
		MailMessage eml = null;

		try (FileInputStream inputStream = new FileInputStream(file)) {
			eml = MailMessage.load(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));

			AttachmentCollection attachments = eml.getAttachments();

			LOGGER.debug("cc : " + toString(eml.getCC()));
			LOGGER.debug("bcc : " + toString(eml.getBcc()));
			LOGGER.debug("sender : " + toString(eml.getSender()));
			LOGGER.debug("from : " + toString(eml.getFrom()));
			LOGGER.debug("to : " + toString(eml.getTo()));
			LOGGER.debug("reversePath : " + toString(eml.getReversePath()));
			LOGGER.debug("subject : " + eml.getSubject());

			MhtSaveOptions options = MhtSaveOptions.getDefaultMhtml();
			options.setMhtFormatOptions(MhtFormatOptions.HideExtraPrintHeader | MhtFormatOptions.WriteHeader | 
					MhtFormatOptions.WriteCompleteBccEmailAddress | MhtFormatOptions.WriteCompleteCcEmailAddress | 
					MhtFormatOptions.WriteCompleteEmailAddress | MhtFormatOptions.WriteCompleteFromEmailAddress | 
					MhtFormatOptions.WriteCompleteToEmailAddress);
			options.setPreserveOriginalDate(true);
			// Overrules the default documentname.
			result.setDocumentName(ConvertorUtil.createTidyNameWithoutExtension(eml.getSubject()));

			File tempMHtmlFile = UniqueFileGenerator.getUniqueFile(getPdfOutputlocation(), this.getClass().getSimpleName(), null);
			eml.getHeaders().set_Item("Date", new SimpleDateFormat(eMailHeaderDateFormat).format(eml.getDate()));
			eml.save(tempMHtmlFile.getAbsolutePath(), options);

			// Load the stream in Word document
			HtmlLoadOptions loadOptions = new HtmlLoadOptions();
			loadOptions.setLoadFormat(LoadFormat.MHTML);
			loadOptions.setWebRequestTimeout(0);

			Long startTime = new Date().getTime();
			try(FileInputStream fis = new FileInputStream(tempMHtmlFile)){
				Document doc = new Document(fis, loadOptions);
				new Fontsetter(cisConversionService.getFontsDirectory()).setFontSettings(doc);
				resizeInlineImages(doc);

				doc.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.PDF);
				
				result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
				Long endTime = new Date().getTime();
				LOGGER.info("Conversion completed in " + (endTime - startTime) + "ms");
			}finally {
				Files.delete(tempMHtmlFile.toPath());
			}
			

			// Convert and (optional add) any attachment of the mail.
			for (int index = 0; index < attachments.size(); index++) {
				// Initialize Attachment object and Get the indexed Attachment reference
				Attachment attachment = attachments.get_Item(index);

				// Convert the attachment.
				CisConversionResult cisConversionResultAttachment = convertAttachmentInPdf(attachment,
						conversionOption);
				// If it is an singlepdf add the file to the the current pdf.
				if ((ConversionOption.SINGLEPDF.equals(conversionOption)) 
					&& (cisConversionResultAttachment.isConversionSuccessfull())) {
					try {
						PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(cisConversionResultAttachment, result.getPdfResultFile());
						pdfAttachmentUtil.addAttachmentInSinglePdf();
					} finally {
						
						deleteFile(cisConversionResultAttachment.getPdfResultFile());
						
						// Clear the file because it is now incorporated in the file it self. 
						cisConversionResultAttachment.setPdfResultFile(null);
						cisConversionResultAttachment.setResultFilePath(null);
					}
					
				}
				result.addAttachment(cisConversionResultAttachment);
			}
		}
			
	}

	private void resizeInlineImages(Document doc) throws Exception {
		Node[] shapes = doc.getChildNodes(NodeType.SHAPE, true).toArray();
		for (int i = 0; i < shapes.length; i++) {
			Shape shape = (Shape) shapes[i];

			// If images needs to be shrunk then scale to fit
			if (shape.getImageData().getImageSize().getWidthPoints() > MaxImageWidthInPoints) {
				double scaleWidth = MaxImageWidthInPoints / shape.getImageData().getImageSize().getWidthPoints();

				double scaleHeight = MaxImageHeightInPoints / shape.getImageData().getImageSize().getHeightPoints();

				// Get the smallest scale factor so it will fit on the paper.
				double scaleFactor = Math.min(scaleWidth, scaleHeight);

				shape.setWidth(shape.getImageData().getImageSize().getWidthPoints() * scaleFactor);
				shape.setHeight(shape.getImageData().getImageSize().getHeightPoints() * scaleFactor);

			}
		}
	}

	/**
	 * Converts an email attachment to a pdf via the cisConversionService.
	 * 
	 * @param attachment
	 * @return
	 * @throws IOException
	 */
	private CisConversionResult convertAttachmentInPdf(Attachment attachment, ConversionOption conversionOption) throws IOException {

		LOGGER.debug("Convert attachment... (" + attachment.getName() + ")");

		// Get the name of the file (segment) (this is the last part.
		String[] segments = attachment.getName().split("/");
		String segmentFilename = segments[segments.length - 1];

		return cisConversionService.convertToPdf(attachment.getContentStream(), segmentFilename, conversionOption);
	}

	private String toString(Iterable<MailAddress> iterable) {
		StringBuilder result = new StringBuilder();

		if (iterable == null) {
			result.append("(null)");
		} else {
			boolean first = true;
			result.append("{Collection:");
			for (MailAddress mailAddress : iterable) {
				if (!first) {
					result.append(", ");
				}
				result.append(toString(mailAddress));
				first = false;
			}
			result.append("}");
		}

		return result.toString();
	}

	private String toString(MailAddress mailAddress) {
		StringBuilder result = new StringBuilder();
		if (mailAddress == null) {
			result.append("(null)");
		} else {
			result.append(" user:");
			result.append(mailAddress.getUser());
			result.append(" address:");
			result.append(mailAddress.getAddress());
			result.append(" originalAddressString:");
			result.append(mailAddress.getOriginalAddressString());
			result.append(" displayName:");
			result.append(mailAddress.getDisplayName());
		}

		return result.toString();
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return false;
	}

}
