package nl.nn.adapterframework.extensions.aspose.pipe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.AsposeLicenseLoader;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.CisConversionServiceImpl;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.PdfAttachmentUtil;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * 
 * @author M64D844
 *
 */
public class PdfPipe extends FixedForwardPipe {

	private static final Logger LOGGER = Logger.getLogger(PdfPipe.class);
	private static final String CONVERSION_OPTION = "CONVERSION_OPTION";
	private boolean saveSeparate = false;
	private CisConversionService cisConversionService;
	private String pdfOutputLocation = null;
	private String fontsDirectory;
	private String license = null;
	private String action = null;
	private List<String> availableActions = Arrays.asList("combine", "convert");
	private String mainDocumentSessionKey = "defaultMainDocumentSessionKey";
	private String fileNameToAttachSessionKey = "defaultFileNameToAttachSessionKey";
	protected String charset = "ISO-8859-1";
	private int sessionkeyPostfixCounter = 0;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(pdfOutputLocation)) {
			try {
				pdfOutputLocation = Files.createTempDirectory("Pdf").toString();
				LOGGER.info("Temporary directory path : " + pdfOutputLocation);
			} catch (IOException e) {
				throw new ConfigurationException(e);
			}
		}

		File outputLocation = new File(pdfOutputLocation);
		if (!outputLocation.exists()) {
			throw new ConfigurationException(
					"Pdf output location does not exists. Please specify an existing location ");
		}

		if (!outputLocation.isDirectory()) {
			throw new ConfigurationException("Pdf output location is not directory. Please specify a diretory");
		}
		if (!availableActions.contains(action)) {
			throw new ConfigurationException(
					"Please specify an action for pdf pipe. Possible values: {convert, combine}");
		}
		AsposeLicenseLoader loader;
		try {
			loader = new AsposeLicenseLoader(license, fontsDirectory);
			loader.loadLicense();
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
		cisConversionService = new CisConversionServiceImpl(pdfOutputLocation, loader.getPathToExtractFonts());

	}

	@Override
	public void start() throws PipeStartException {
		super.start();

	}

	@Override
	public void stop() {
		try {
			Files.delete(Paths.get(pdfOutputLocation));
		} catch (IOException e) {
			LOGGER.debug("Could not delete the temp folder " + pdfOutputLocation);
		}
		super.stop();
	}

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		long start = new Date().getTime();
		InputStream binaryInputStream = null;
		if (input instanceof InputStream) {
			binaryInputStream = (InputStream) input;
		} else if (input instanceof byte[]) {
			binaryInputStream = new ByteArrayInputStream((byte[]) input);
		} else {
			try {
				binaryInputStream = (input == null) ? null
						: new ByteArrayInputStream(input.toString().getBytes(charset));
			} catch (UnsupportedEncodingException e) {
				throw new PipeRunException(this,
						getLogPrefix(session) + "cannot encode message using charset [" + getCharset() + "]", e);
			}
		}

		if ("combine".equalsIgnoreCase(action)) {
			// Get main document to attach attachments
			InputStream mainPdf = null;
			Object mainPdfObject = session.get(mainDocumentSessionKey);
			if (mainPdfObject instanceof InputStream) {
				mainPdf = (InputStream) mainPdfObject;
			} else if (mainPdfObject instanceof byte[]) {
				mainPdf = new ByteArrayInputStream((byte[]) mainPdfObject);
			} else {
				try {
					mainPdf = (mainPdfObject == null) ? null
							: new ByteArrayInputStream(mainPdfObject.toString().getBytes(charset));
				} catch (UnsupportedEncodingException e) {
					throw new PipeRunException(this,
							getLogPrefix(session) + "cannot encode message using charset [" + getCharset() + "]", e);
				}
			}
			// Get file name of attachment
			String fileNameToAttach = (String) session.get(fileNameToAttachSessionKey);

			InputStream result = PdfAttachmentUtil.combineFiles(mainPdf, binaryInputStream, fileNameToAttach + ".pdf");

			session.put(CONVERSION_OPTION, ConversionOption.SINGLEPDF);
			session.put(mainDocumentSessionKey, result);

		} else if ("convert".equalsIgnoreCase(action)) {
			String fileName = (String) session.get("fileName");
			// String conversionOption = (String) session.get("attachmentOption");
			long tsBeforeConvert = new Date().getTime();
			CisConversionResult cisConversionResult = cisConversionService.convertToPdf(binaryInputStream, fileName,
					ConversionOption.SEPERATEPDF);
			long tsAfterConvert = new Date().getTime();
			System.err.println("PDFPipe cisConversionService.convertToPdf( takes ::::: "
					+ (tsAfterConvert - tsBeforeConvert) + " ms");
			System.err.println(cisConversionResult.toString());

			if (cisConversionResult.isConversionSuccessfull()) {
				System.err.println("Conversion was successful");
			}
			session.put(CONVERSION_OPTION, cisConversionResult.getConversionOption().getValue());
			session.put("MEDIA_TYPE", cisConversionResult.getMediaType().toString());
			session.put("DOCUMENT_NAME", cisConversionResult.getDocumentName());
			session.put("FAILURE_REASON", cisConversionResult.getFailureReason());
			session.put("PARENT_CONVERSION_ID", null);
			session.put("CONVERTED_DOCUMENT", cisConversionResult.getFileStream());

			XmlBuilder main = new XmlBuilder("main");
			main.addAttribute(CONVERSION_OPTION, cisConversionResult.getConversionOption().getValue());
			main.addAttribute("MEDIA_TYPE", cisConversionResult.getMediaType().toString());
			main.addAttribute("DOCUMENT_NAME", cisConversionResult.getDocumentName());
			main.addAttribute("FAILURE_REASON", cisConversionResult.getFailureReason());
			main.addAttribute("PARENT_CONVERSION_ID", null);
			main.addAttribute("CONVERTED_DOCUMENT_SESSION_KEY", "main");

			buildXmlFromResult(main, cisConversionResult, session);
			session.put("documents", main.toXML());
			sessionkeyPostfixCounter = 0;
		}
		long end = new Date().getTime();
		System.err.println("PDFPipe doPipe takes ::::: " + (end - start) + " ms");
		LOGGER.error("PDFPipe doPipe takes ::::: " + (end - start) + " ms");
		return new PipeRunResult(getForward(), "");
	}

	private void buildXmlFromResult(XmlBuilder main, CisConversionResult cisConversionResult,
			IPipeLineSession session) {

		List<CisConversionResult> attachments = cisConversionResult.getAttachments();
		if (attachments != null && !attachments.isEmpty()) {
			XmlBuilder attachmentsAsXml = new XmlBuilder("attachments");
			for (int i = 0; i < attachments.size(); i++) {
				CisConversionResult attachment = attachments.get(i);

				XmlBuilder attachmentAsXml = new XmlBuilder("attachment");
				attachmentAsXml.addAttribute("conversionOption", attachment.getConversionOption().getValue() + "");
				attachmentAsXml.addAttribute("mediaType", attachment.getMediaType().toString());
				attachmentAsXml.addAttribute("documentName", attachment.getDocumentName());
				attachmentAsXml.addAttribute("failureReason", attachment.getFailureReason());
				attachmentAsXml.addAttribute("convertedDocumentSessionKey", sessionkeyPostfixCounter + "");
				session.put(sessionkeyPostfixCounter + "", attachment.getFileStream());
				attachmentsAsXml.addSubElement(attachmentAsXml);
				sessionkeyPostfixCounter++;

				buildXmlFromResult(attachmentAsXml, attachment, session);
			}
			main.addSubElement(attachmentsAsXml);
		}
	}

	public boolean isSaveSeparate() {
		return saveSeparate;
	}

	public void setSaveSeparate(boolean saveSeparate) {
		this.saveSeparate = saveSeparate;
	}

	public String getPdfOutputLocation() {
		return pdfOutputLocation;
	}

	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputLocation = pdfOutputLocation;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getMainDocumentSessionKey() {
		return mainDocumentSessionKey;
	}

	public void setMainDocumentSessionKey(String mainDocumentSessionKey) {
		this.mainDocumentSessionKey = mainDocumentSessionKey;
	}

	public String getFileNameToAttachSessionKey() {
		return fileNameToAttachSessionKey;
	}

	public void setFileNameToAttachSessionKey(String fileNameToAttachSessionKey) {
		this.fileNameToAttachSessionKey = fileNameToAttachSessionKey;
	}

	public String getFontsDirectory() {
		return fontsDirectory;
	}

	public void setFontsDirectory(String fontsDirectory) {
		this.fontsDirectory = fontsDirectory;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}
}
