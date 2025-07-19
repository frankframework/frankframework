/*
   Copyright 2019, 2021-2023 WeAreFrank!

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

import com.aspose.email.Attachment;
import com.aspose.email.AttachmentCollection;
import com.aspose.email.EmlLoadOptions;
import com.aspose.email.LoadOptions;
import com.aspose.email.MailAddress;
import com.aspose.email.MailMessage;
import com.aspose.email.MhtFormatOptions;
import com.aspose.email.MhtSaveOptions;
import com.aspose.email.MsgLoadOptions;
import com.aspose.email.SaveOptions;
import com.aspose.email.TnefLoadOptions;
import com.aspose.words.Document;
import com.aspose.words.HtmlLoadOptions;
import com.aspose.words.LoadFormat;
import com.aspose.words.Node;
import com.aspose.words.NodeType;
import com.aspose.words.SaveFormat;
import com.aspose.words.Shape;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.ConversionOption;
import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.extensions.aspose.services.conv.CisConversionService;
import org.frankframework.extensions.aspose.services.util.ConvertorUtil;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.DateFormatUtils;

@Log4j2
class MailConvertor extends AbstractConvertor {

	private static final float MAX_IMAGE_WIDTH_IN_POINTS = PageConvertUtil.convertCmToPoints(PageConvertUtil.PAGE_WIDHT_IN_CM - 2 * 1.1f);
	private static final float MAX_IMAGE_HEIGHT_IN_POINTS = PageConvertUtil.convertCmToPoints(PageConvertUtil.PAGE_HEIGTH_IN_CM - 2 * 1.1f);
	private static final String MAIL_HEADER_DATEFORMAT = "dd-MM-yyyy HH:mm:ss";
	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateFormatUtils.getDateTimeFormatterWithOptionalComponents(MAIL_HEADER_DATEFORMAT);
	private final CisConversionService cisConversionService;

	// contains mapping from MediaType to the LoadOption for the Aspose Word conversion.
	private static final Map<MediaType, Class<? extends LoadOptions>> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		final Map<MediaType, Class<? extends LoadOptions>> map = new HashMap<>();
		map.put(new MediaType("message", "rfc822"), EmlLoadOptions.class);
		map.put(new MediaType("message", "rfc822.concept"), EmlLoadOptions.class);
		map.put(new MediaType("message", "rfc822.ddcim"), EmlLoadOptions.class);
		map.put(new MediaType("application", "vnd.ms-outlook"), MsgLoadOptions.class);
		map.put(new MediaType("application", "vnd.ms-tnef"), TnefLoadOptions.class);

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	private static final MhtSaveOptions options = SaveOptions.getDefaultMhtml();
	static {
		options.setMhtFormatOptions(MhtFormatOptions.HideExtraPrintHeader | MhtFormatOptions.WriteHeader |
				MhtFormatOptions.WriteCompleteBccEmailAddress | MhtFormatOptions.WriteCompleteCcEmailAddress |
				MhtFormatOptions.WriteCompleteEmailAddress | MhtFormatOptions.WriteCompleteFromEmailAddress |
				MhtFormatOptions.WriteCompleteToEmailAddress);
		options.setPreserveOriginalDate(true);
	}

	protected MailConvertor(CisConversionService cisConversionService, CisConfiguration configuration) {
		super(configuration, MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet());
		this.cisConversionService = cisConversionService;
	}

	@Override
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {
		MailMessage eml;

		try (InputStream inputStream = message.asInputStream(charset)) {
			eml = MailMessage.load(inputStream, ClassUtils.newInstance(MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType)));
		}

		AttachmentCollection attachments = eml.getAttachments();

		if (log.isDebugEnabled()) {
			log.debug("cc : [{}]", toString(eml.getCC()));
			log.debug("bcc : [{}]", toString(eml.getBcc()));
			log.debug("sender : [{}]", toString(eml.getSender()));
			log.debug("from : [{}]", toString(eml.getFrom()));
			log.debug("to : [{}]", toString(eml.getTo()));
			log.debug("subject : [{}]", eml.getSubject());
		}

		// Overrules the default documentname.
		result.setDocumentName(ConvertorUtil.createTidyNameWithoutExtension(eml.getSubject()));

		File tempMHtmlFile = UniqueFileGenerator.getUniqueFile(configuration.getPdfOutputLocation(), this.getClass().getSimpleName(), null);
		String date = DATE_TIME_FORMATTER.format(eml.getDate().toInstant());
		eml.getHeaders().set_Item("Date", date);
		eml.save(tempMHtmlFile.getAbsolutePath(), options);

		// Load the stream in Word document
		HtmlLoadOptions loadOptions = new HtmlLoadOptions();
		loadOptions.setLoadFormat(LoadFormat.MHTML);
		loadOptions.setWebRequestTimeout(0);
		if(!configuration.isLoadExternalResources()){
			loadOptions.setResourceLoadingCallback(new OfflineResourceLoader());
		}

		Long startTime = System.currentTimeMillis();
		try(FileInputStream fis = new FileInputStream(tempMHtmlFile)){
			Document doc = new Document(fis, loadOptions);
			new FontManager(configuration.getFontsDirectory()).setFontSettings(doc);
			resizeInlineImages(doc);

			doc.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.PDF);

			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
			Long endTime = System.currentTimeMillis();
			log.debug("document converted in [{}ms]", (endTime - startTime));
		} finally {
			Files.delete(tempMHtmlFile.toPath());
		}

		// Convert and (optional add) any attachment of the mail.
		for (int index = 0; index < attachments.size(); index++) {
			// Initialize Attachment object and Get the indexed Attachment reference
			Attachment attachment = attachments.get_Item(index);

			// Convert the attachment.
			CisConversionResult cisConversionResultAttachment = convertAttachmentInPdf(attachment, result.getConversionOption());
			// If it is a singlepdf add the file to the current pdf.
			if (ConversionOption.SINGLEPDF.equals(result.getConversionOption()) && cisConversionResultAttachment.isConversionSuccessful()) {
				try {
					PdfAttachmentUtil pdfAttachmentUtil = new PdfAttachmentUtil(cisConversionResultAttachment, result.getPdfResultFile());
					pdfAttachmentUtil.addAttachmentInSinglePdf();
				} finally {
					deleteFile(cisConversionResultAttachment.getPdfResultFile());
					// Clear the file because it is now incorporated in the file itself.
					cisConversionResultAttachment.setPdfResultFile(null);
					cisConversionResultAttachment.setResultFilePath(null);
				}

			}
			result.addAttachment(cisConversionResultAttachment);
		}
	}

	private void resizeInlineImages(Document doc) throws Exception {
		Node[] shapes = doc.getChildNodes(NodeType.SHAPE, true).toArray();
		for (Node node : shapes) {
			Shape shape = (Shape) node;

			// If images needs to be shrunk then scale to fit
			if (shape.getWidth() > MAX_IMAGE_WIDTH_IN_POINTS || shape.getHeight() > MAX_IMAGE_HEIGHT_IN_POINTS) {

				// make sure that aspect ratio is locked
				if (!shape.getAspectRatioLocked()) {
					shape.setAspectRatioLocked(true);
				}

				if (shape.getWidth() > MAX_IMAGE_WIDTH_IN_POINTS) {
					shape.setWidth(scaleDimension(shape.getWidth(), MAX_IMAGE_WIDTH_IN_POINTS));
				}
				if (shape.getHeight() > MAX_IMAGE_HEIGHT_IN_POINTS) {
					shape.setHeight(scaleDimension(shape.getHeight(), MAX_IMAGE_HEIGHT_IN_POINTS));
				}
			}
		}
	}

	private double scaleDimension(Double currentValue, float maxValue){
		double scaleFactor = maxValue / currentValue;
		return scaleFactor * currentValue;
	}

	/**
	 * Converts an email attachment to a pdf via the cisConversionService.
	 */
	private CisConversionResult convertAttachmentInPdf(Attachment attachment, ConversionOption conversionOption) throws IOException {
		log.debug("convert attachment [{}]", attachment::getName);

		// Get the name of the file (segment) (this is the last part).
		String[] segments = attachment.getName().split("/");
		String segmentFilename = segments[segments.length - 1];

		return cisConversionService.convertToPdf(new Message(attachment.getContentStream()), segmentFilename, conversionOption);
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
