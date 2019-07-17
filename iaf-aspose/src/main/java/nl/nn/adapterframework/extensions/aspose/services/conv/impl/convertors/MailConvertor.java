package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
import com.aspose.email.MhtMessageFormatter;
import com.aspose.email.MhtSaveOptions;
import com.aspose.email.SaveOptions;
import com.aspose.email.TnefLoadOptions;
import com.aspose.pdf.FileSpecification;
import com.aspose.pdf.PageMode;
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
import nl.nn.adapterframework.extensions.aspose.services.conv.MailMetaData;
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.ConvertorUtil;
import nl.nn.adapterframework.extensions.aspose.services.util.StringsUtil;

class MailConvertor extends AbstractConvertor {

	private static final Logger LOGGER = Logger.getLogger(MailConvertor.class);

	private static final float MaxImageWidthInPoints = PageConvertUtil
			.convertCmToPoints(PageConvertUtil.PAGE_WIDHT_IN_CM - 2 * 1.1f);
	private static final float MaxImageHeightInPoints = PageConvertUtil
			.convertCmToPoints(PageConvertUtil.PAGE_HEIGTH_IN_CM - 2 * 1.1f);

	private CisConversionService cisConversionService;

	// contains mapping from MediaType to the LoadOption for the aspose word conversion.
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
	void convert(MediaType mediaType, InputStream inputStream, File fileDest, CisConversionResult result,
			ConversionOption conversionOption) throws Exception {

		MailMessage eml = MailMessage.load(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));

		LOGGER.info("Convert mail with messageId: " + eml.getMessageId() + ", from: " + eml.getFrom() + ", subject: "
				+ eml.getSubject());

		AttachmentCollection attachments = eml.getAttachments();

		LOGGER.debug("cc          : " + toString(eml.getCC()));
		LOGGER.debug("bcc         : " + toString(eml.getBcc()));
		LOGGER.debug("sender      : " + toString(eml.getSender()));
		LOGGER.debug("from        : " + toString(eml.getFrom()));
		LOGGER.debug("sent on     : " + toString(eml.getLocalDate()));
		LOGGER.debug("to          : " + toString(eml.getTo()));
		LOGGER.debug("reversePath : " + toString(eml.getReversePath()));
		LOGGER.debug("subject     : " + eml.getSubject());

		// Change mail headers (From, Sent, To, ...) to Dutch (also MhtFormatOptions on None so the English headers are suppressed).
		MhtMessageFormatter messageformatter = new MhtMessageFormatter();
		messageformatter.setFromFormat(messageformatter.getFromFormat().replace("From:", "Afzender:"));
		messageformatter.setSentFormat(messageformatter.getSentFormat().replace("Sent:", "Verzonden:"));
		messageformatter.setSubjectFormat(messageformatter.getSubjectFormat().replace("Subject:", "Onderwerp:"));
		messageformatter.setToFormat(messageformatter.getToFormat().replace("To:", "Aan:"));
		messageformatter.setCcFormat(messageformatter.getCcFormat().replace("??:", "Cc:"));
		messageformatter
				.setImportanceFormat(messageformatter.getImportanceFormat().replace("Importance:", "Belangrijk:"));
		messageformatter.setBccFormat(messageformatter.getBccFormat().replace("Bcc:", "Bcc:"));
		messageformatter
				.setAttachmentFormat(messageformatter.getAttachmentFormat().replace("Attachments:", "Attachments:"));
		messageformatter.format(eml);

		MailAddress from = eml.getFrom();

		// Overrules the default documentname.
		result.setDocumentName(ConvertorUtil.createTidyNameWithoutExtension(eml.getSubject()));

		// Save the Message to output stream in MHTML format
		ByteArrayOutputStream emlStream = new ByteArrayOutputStream();
		MhtSaveOptions saveOptions = SaveOptions.getDefaultMhtml();
		saveOptions.setMhtFormatOptions(MhtFormatOptions.None);
		eml.save(emlStream, saveOptions);

		// Load the stream in Word document		
		HtmlLoadOptions loadOptions = new HtmlLoadOptions();
		loadOptions.setLoadFormat(LoadFormat.MHTML);
		loadOptions.setWebRequestTimeout(0);

		Document doc = new Document(new ByteArrayInputStream(emlStream.toByteArray()), loadOptions);
		new Fontsetter().setFontSettings(doc);
		resizeInlineImages(doc);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		doc.save(outputStream, SaveFormat.PDF);
		InputStream inStream = new ByteArrayInputStream(outputStream.toByteArray());
		result.setFileStream(inStream);
		
//		result.setMetaData(new MailMetaData(getNumberOfPages(inStream), toUserFormat(from), eml.getLocalDate(),
//				toUserFormat(eml.getTo()), eml.getSubject()));

		// Convert and (optional add) any attachment of the mail.
		for (int index = 0; index < attachments.size(); index++) {
			// Initialize Attachment object and Get the indexed Attachment reference
			Attachment attachment = attachments.get_Item(index);

			// Convert the attachment.
			CisConversionResult cisConversionResultAttachment = convertAttachmentInPdf(attachment, conversionOption);
			LOGGER.debug("Mail attachment : " + cisConversionResultAttachment);

			// If it is an singlepdf add the file to the the current pdf.
			if ((ConversionOption.SINGLEPDF.equals(conversionOption))
					&& (cisConversionResultAttachment.isConversionSuccessfull())) {
				// If conversion successful add the converted pdf to the pdf.
				addAttachmentInPdf(cisConversionResultAttachment, fileDest);
			}
			// Add the conversion information to the result (even if the conversion failed!).
			result.addAttachment(cisConversionResultAttachment);
		}

	}

	private void resizeInlineImages(Document doc) throws Exception {
		Node[] shapes = doc.getChildNodes(NodeType.SHAPE, true).toArray();
		for (int i = 0; i < shapes.length; i++) {
			Shape shape = (Shape) shapes[i];

			//If images needs to be shrunk then scale to fit
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
	 * @param attachment
	 * @return
	 * @throws IOException 
	 */
	private CisConversionResult convertAttachmentInPdf(Attachment attachment, ConversionOption conversionOption)
			throws IOException {

		LOGGER.debug("Convert attachment... (" + attachment.getName() + ")");

		// Get the name of the file (segment) (this is the last part.
		String[] segments = attachment.getName().split("/");
		String segmentFilename = segments[segments.length - 1];

		// Create a bufferedInputStream because that support markSupported as is required for the tika library.
		try (InputStream inputStream = new BufferedInputStream(attachment.getContentStream())) {
			// Convert the attachment to pdf.
			return cisConversionService.convertToPdf(inputStream, segmentFilename, conversionOption);
		} finally {
			LOGGER.debug("Convert attachment finished. (" + attachment.getName() + ")");
		}
	}

	/**
	 * Adds a file (in cisConversieResultBuilder) to the given (fileCombined).
	 * @param cisConversieResultBuilder
	 * @param fileCombined
	 * @throws IOException
	 */
	// TODO GH Merge deze operatie met PdfAttachmentUtil.addAttachmentInSinglePdf
	private void addAttachmentInPdf(CisConversionResult cisConversieResult, File fileCombined) throws IOException {

		LOGGER.debug("Adding attachment... (" + cisConversieResult.getDocumentName() + ")");

		if (!cisConversieResult.isConversionSuccessfull()) {
			throw new IllegalArgumentException("Only successfull converted files can be added!");
		}

		// Open a document
		com.aspose.pdf.Document pdfDocument = new com.aspose.pdf.Document(fileCombined.getAbsolutePath());
		String documentName = ConvertorUtil.createTidyPdfFilename(cisConversieResult.getDocumentName());

		LOGGER.debug("Adding attachments... (" + documentName + ")");

		try (InputStream attachmentDocumentStream = new BufferedInputStream(
				new FileInputStream(cisConversieResult.getPdfResultFile()))) {

			// Set up a new file to be added as attachment
			FileSpecification fileSpecification = new FileSpecification(attachmentDocumentStream, documentName);

			// Add an attachment to document's attachment collection
			pdfDocument.getEmbeddedFiles().add(fileSpecification);

			// UseOC means "Optional attachments panel set to visible" used so that the attachments are shown.
			pdfDocument.setPageMode(PageMode.UseAttachments);

			// Save the updated document
			pdfDocument.save();
		} finally {
			pdfDocument.freeMemory();
			pdfDocument.dispose();
			pdfDocument.close();

			deleteFile(cisConversieResult.getPdfResultFile());

			// Clear the file because it is now incorporated in the file it self. 
			cisConversieResult.setPdfResultFile(null);
		}

		LOGGER.debug("Adding attachment finished. (" + documentName + ")");
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

	private String toUserFormat(Iterable<MailAddress> iterable) {
		StringBuilder result = new StringBuilder();

		if (iterable != null) {
			boolean first = true;
			for (MailAddress mailAddress : iterable) {
				if (!first) {
					result.append(", ");
				}
				result.append(toUserFormat(mailAddress));
				first = false;
			}
		}

		return result.toString();
	}

	/**
	 * Adds first the displayname or user name when available. Adds than the email address (the original or when not available the address).
	 * @param mailAddress
	 * @return
	 */
	private String toUserFormat(MailAddress mailAddress) {
		StringBuilder result = new StringBuilder();
		if (mailAddress != null) {
			if (!StringsUtil.isBlank(mailAddress.getAddress())) {
				result.append(mailAddress.getAddress());
			} else if (!StringsUtil.isBlank(mailAddress.getOriginalAddressString())) {
				result.append(mailAddress.getOriginalAddressString());
			}
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
