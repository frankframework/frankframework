package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertorUtil;

class MailConvertor extends AbstractConvertor {

	private static final Logger LOGGER = Logger.getLogger(MailConvertor.class);

	private static final float MaxImageWidthInPoints = PageConvertUtil
			.convertCmToPoints(PageConvertUtil.PAGE_WIDHT_IN_CM - 2 * 1.1f);
	private static final float MaxImageHeightInPoints = PageConvertUtil
			.convertCmToPoints(PageConvertUtil.PAGE_HEIGTH_IN_CM - 2 * 1.1f);

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
			LOGGER.debug("sent on : " + toString(eml.getLocalDate()));
			LOGGER.debug("to : " + toString(eml.getTo()));
			LOGGER.debug("reversePath : " + toString(eml.getReversePath()));
			LOGGER.debug("subject : " + eml.getSubject());

			MhtSaveOptions options = MhtSaveOptions.getDefaultMhtml();
			options.setMhtFormatOptions(MhtFormatOptions.WriteHeader);
//			options.getFormatTemplates().get_Item("From").replace("From:", "Afzender:");
//			options.getFormatTemplates().get_Item("Sent").replace("Sent:", "Verzonden:");
//			options.getFormatTemplates().get_Item("Subject").replace("Subject:", "Onderwerp:");
//			options.getFormatTemplates().get_Item("Importance").replace("To:", "Aan:");
//			options.getFormatTemplates().get_Item("Cc").replace("Cc:", "Afzender:");
			// Overrules the default documentname.
			result.setDocumentName(ConvertorUtil.createTidyNameWithoutExtension(eml.getSubject()));

			// Save the Message to output stream in MHTML format
			ByteArrayOutputStream emlStream = new ByteArrayOutputStream();

			eml.save(emlStream, options);

			// Load the stream in Word document
			HtmlLoadOptions loadOptions = new HtmlLoadOptions();
			loadOptions.setLoadFormat(LoadFormat.MHTML);
			loadOptions.setWebRequestTimeout(0);

			Long startTime = new Date().getTime();
			Document doc = new Document(new ByteArrayInputStream(emlStream.toByteArray()), loadOptions);
			emlStream.close();
			new Fontsetter(cisConversionService.getFontsDirectory()).setFontSettings(doc);
			resizeInlineImages(doc);

			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			long start = new Date().getTime();
			doc.save(outputStream, SaveFormat.PDF);
			long end = new Date().getTime();
			
			LOGGER.info("Conversion(save operation in convert method) takes  :::  " + (end - start) + " ms");
			
			InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
			result.setFileStream(inStream);
			outputStream.close();
			long endTime = new Date().getTime();
			
			LOGGER.info("Conversion completed in " + (endTime - startTime) + "ms");

			// Convert and (optional add) any attachment of the mail.
			List<CisConversionResult> convertedAttachments = new ArrayList<>();
			for (int index = 0; index < attachments.size(); index++) {
				// Initialize Attachment object and Get the indexed Attachment reference
				Attachment attachment = attachments.get_Item(index);

				// Convert the attachment.
				CisConversionResult cisConversionResultAttachment = convertAttachmentInPdf(attachment,
						conversionOption);
				if ((ConversionOption.SINGLEPDF.equals(conversionOption)) 
						&& (cisConversionResultAttachment.isConversionSuccessfull())) {
						// If conversion successful add the converted pdf to the pdf.
						convertedAttachments.add(cisConversionResultAttachment);
				}
				result.addAttachment(cisConversionResultAttachment);
			}
			if(!convertedAttachments.isEmpty()) {
				PdfAttachmentUtil.addAttachmentInSinglePdf(convertedAttachments, result);
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

	private String toString(Date date) {
		if (date == null) {
			return "(null)";
		} else {
			return ConvertUtil.convertTimestampToStr(date);
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return false;
	}

}
