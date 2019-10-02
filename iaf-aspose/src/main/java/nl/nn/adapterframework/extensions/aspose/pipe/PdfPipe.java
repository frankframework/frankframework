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
package nl.nn.adapterframework.extensions.aspose.pipe;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.AsposeLicenseLoader;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.CisConversionServiceImpl;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.PdfAttachmentUtil;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Converts files to pdf type. This pipe has two actions convert and combine. 
 * With combine action you can attach files into main pdf file. 
 * @author M64D844
 *
 */
public class PdfPipe extends FixedForwardPipe {

	private static final Logger LOGGER = Logger.getLogger(PdfPipe.class);
	private static final String CONVERSION_OPTION = "CONVERSION_OPTION";
	private boolean saveSeparate = false;
	private String pdfOutputLocation = null;
	private String fontsDirectory;
	private String license = null;
	private String action = null;
	private List<String> availableActions = Arrays.asList("combine", "convert");
	private String mainDocumentSessionKey = "defaultMainDocumentSessionKey";
	private String fileNameToAttachSessionKey = "defaultFileNameToAttachSessionKey";
	protected String charset = "UTF-8";
	private boolean isTempDirCreated = false;
	private AsposeLicenseLoader loader;
	
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if(StringUtils.isNotEmpty(pdfOutputLocation)) {
			File outputLocation = new File(pdfOutputLocation);
			if (!outputLocation.exists()) {
				throw new ConfigurationException(
						"Pdf output location does not exist. Please specify an existing location ");
			}

			if (!outputLocation.isDirectory()) {
				throw new ConfigurationException("Pdf output location is not directory. Please specify a diretory");
			}
		}
		
		if (!availableActions.contains(action)) {
			throw new ConfigurationException(
					"Please specify an action for pdf pipe. Possible values: {convert, combine}");
		}

		// TODO: could be used without a license with a evaluation watermark on the converted file
		// License check
		if (StringUtils.isEmpty(license)) {
			throw new ConfigurationException("Please specify the full path to aspose license including file name.");
		}
		try (InputStream inputStream = new FileInputStream(license)) {

		} catch (FileNotFoundException notFound) {
			throw new ConfigurationException("Specified file for aspose license is not found", notFound);
		} catch (IOException e1) {
			throw new ConfigurationException(e1);
		}
		// load license
		try {
			loader = new AsposeLicenseLoader(license, fontsDirectory);
			loader.loadLicense();
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		if (StringUtils.isEmpty(pdfOutputLocation)) {
			try {
				pdfOutputLocation = Files.createTempDirectory("Pdf").toString();
				LOGGER.info("Temporary directory path : " + pdfOutputLocation);
				isTempDirCreated = true;
			} catch (IOException e) {
				throw new PipeStartException(e);
			}
		}
		
	}

	@Override
	public void stop() {
		if(isTempDirCreated) {
			try {
				Files.delete(Paths.get(pdfOutputLocation));
				LOGGER.info("Temporary directory is deleted : " + pdfOutputLocation);
				pdfOutputLocation="";
			} catch (IOException e) {
				LOGGER.debug("Could not delete the temp folder " + pdfOutputLocation);
			}
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
			CisConversionResult cisConversionResult = null;
			try {
				CisConversionService cisConversionService = new CisConversionServiceImpl(pdfOutputLocation, loader.getPathToExtractFonts());
				cisConversionResult = cisConversionService.convertToPdf(binaryInputStream, fileName,
						saveSeparate ? ConversionOption.SEPARATEPDF : ConversionOption.SINGLEPDF);
				XmlBuilder main = new XmlBuilder("main");
				main.addAttribute(CONVERSION_OPTION, cisConversionResult.getConversionOption().getValue());
				main.addAttribute("MEDIA_TYPE", cisConversionResult.getMediaType().toString());
				main.addAttribute("DOCUMENT_NAME", cisConversionResult.getDocumentName());
				main.addAttribute("FAILURE_REASON", cisConversionResult.getFailureReason());
				main.addAttribute("PARENT_CONVERSION_ID", null);
				main.addAttribute("NUMBER_OF_PAGES", cisConversionResult.getNumberOfPages());
				main.addAttribute("CONVERTED_DOCUMENT", cisConversionResult.getResultFilePath());

				buildXmlFromResult(main, cisConversionResult, session);
				session.put("documents", main.toXML());
			} catch (IOException e) {
				throw new PipeRunException(this, "", e);
			}
			
		}
		long end = new Date().getTime();
		LOGGER.info("PDFPipe doPipe takes ::::: " + (end - start) + " ms");
		return new PipeRunResult(getForward(), "");
	}
	/**
	 * Creates and xml containing conversion results both attachments and the main document.
	 * @param main
	 * @param cisConversionResult
	 * @param session
	 */
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
				attachmentAsXml.addAttribute("numberOfPages", attachment.getNumberOfPages());
				attachmentAsXml.addAttribute("filePath", attachment.getResultFilePath());
				attachmentsAsXml.addSubElement(attachmentAsXml);

				buildXmlFromResult(attachmentAsXml, attachment, session);
			}
			main.addSubElement(attachmentsAsXml);
		}
	}

	public String getAction() {
		return action;
	}

	@IbisDoc({ "action to be processed by pdf pipe possible values:{combine, convert}", "null" })
	public void setAction(String action) {
		this.action = action;
	}

	public String getMainDocumentSessionKey() {
		return mainDocumentSessionKey;
	}

	@IbisDoc({
			"session key that contains the document that the attachments will be attached to. Only used when action is set to 'combine'", "defaultMainDocumentSessionKey" })
	public void setMainDocumentSessionKey(String mainDocumentSessionKey) {
		this.mainDocumentSessionKey = mainDocumentSessionKey;
	}

	public String getFileNameToAttachSessionKey() {
		return fileNameToAttachSessionKey;
	}

	@IbisDoc({ "session key that contains the filename to be attached. Only used when the action is set to 'combine' ", "defaultFileNameToAttachSessionKey" })
	public void setFileNameToAttachSessionKey(String fileNameToAttachSessionKey) {
		this.fileNameToAttachSessionKey = fileNameToAttachSessionKey;
	}

	public String getFontsDirectory() {
		return fontsDirectory;
	}

	@IbisDoc({"fonts folder to load the fonts. If not set then a temporary folder will be created to extract fonts from fonts.zip everytime. Having fontsDirectory to be set will improve startup time", "null" })
	public void setFontsDirectory(String fontsDirectory) {
		this.fontsDirectory = fontsDirectory;
	}

	public String getCharset() {
		return charset;
	}

	@IbisDoc({ "charset to be used to encode the given input string ", "UTF-8" })
	public void setCharset(String charset) {
		this.charset = charset;
	}

	public String getLicense() {
		return license;
	}

	@IbisDoc({ "aspose license location including file name. Must be specified.", "" })
	public void setLicense(String license) {
		this.license = license;
	}

	public boolean isSaveSeparate() {
		return saveSeparate;
	}
	@IbisDoc({ "when sets to false, converts the file including the attachments attached to the main file. when it is true, saves each attachment separately", "false" })
	public void setSaveSeparate(boolean saveSeparate) {
		this.saveSeparate = saveSeparate;
	}

	public String getPdfOutputLocation() {
		return pdfOutputLocation;
	}
	
	@IbisDoc({ "directory to save resulting pdf files after conversion. If not set then a temporary directory will be created and the conversion results will be stored in that directory.", "null" })
	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputLocation = pdfOutputLocation;
	}
}
