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
package nl.nn.adapterframework.extensions.aspose.pipe;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.aspose.AsposeFontManager;
import nl.nn.adapterframework.extensions.aspose.AsposeLicenseLoader;
import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.CisConversionServiceImpl;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.PdfAttachmentUtil;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Converts files to pdf type. This pipe has two actions convert and combine. 
 * With combine action you can attach files into main pdf file. 
 *
 */
public class PdfPipe extends FixedForwardPipe {

	private boolean saveSeparate = false;
	private String pdfOutputLocation = null;
	private String fontsDirectory;
	private String license = null;
	private String action = null;
	private List<String> availableActions = Arrays.asList("combine", "convert");
	private String mainDocumentSessionKey = "defaultMainDocumentSessionKey";
	private String filenameToAttachSessionKey = "defaultFileNameToAttachSessionKey";
	protected String charset = null;
	private boolean isTempDirCreated = false;
	private AsposeFontManager fontManager;
	private boolean unpackDefaultFonts = false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if(StringUtils.isNotEmpty(pdfOutputLocation)) {
			File outputLocation = new File(pdfOutputLocation);
			if (!outputLocation.exists()) {
				throw new ConfigurationException("pdf output location does not exist");
			}

			if (!outputLocation.isDirectory()) {
				throw new ConfigurationException("pdf output location is not a valid directory");
			}
		}

		if (!availableActions.contains(action)) {
			throw new ConfigurationException("please specify an action for pdf pipe. possible values: {convert, combine}");
		}

		if (StringUtils.isEmpty(license)) {
			ConfigurationWarnings.add(this, log, "Aspose License is not configured. There will be evaluation watermarks on the converted documents. There are also some restrictions in the API use. License field should be set with a valid information to avoid this. ");
		} else {
			URL licenseUrl = ClassUtils.getResourceURL(license);
			if(licenseUrl == null) {
				throw new ConfigurationException("specified file for aspose license is not found");
			}

			try {
				AsposeLicenseLoader.loadLicenses(licenseUrl);
			} catch (Exception e) {
				throw new ConfigurationException("an error occured while loading Aspose license(s)");
			}
		}

		fontManager = new AsposeFontManager(fontsDirectory);
		try {
			fontManager.load(unpackDefaultFonts);
		} catch (IOException e) {
			throw new ConfigurationException("an error occured while loading fonts", e);
		}
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		if (StringUtils.isEmpty(pdfOutputLocation)) {
			try {
				//TODO uniform temp dir (ibis.tmpdir) should be used
				pdfOutputLocation = Files.createTempDirectory("Pdf").toString();
				log.info("Temporary directory path : " + pdfOutputLocation);
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
				Files.delete(Paths.get(pdfOutputLocation));//temp file should be removed after use, not when the ibis shuts down
				log.info("Temporary directory is deleted : " + pdfOutputLocation);
				pdfOutputLocation=null;
			} catch (IOException e) {
				log.debug("Could not delete the temp folder " + pdfOutputLocation);
			}
		}
		super.stop();
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		try (InputStream binaryInputStream = input.asInputStream(charset)) {

			if ("combine".equalsIgnoreCase(action)) {
				// Get main document to attach attachments
				InputStream mainPdf = Message.asInputStream(session.get(mainDocumentSessionKey), charset);
				// Get file name of attachment
				String fileNameToAttach = Message.asString(session.get(getFilenameToAttachSessionKey()));

				InputStream result = PdfAttachmentUtil.combineFiles(mainPdf, binaryInputStream, fileNameToAttach + ".pdf");

				session.put("CONVERSION_OPTION", ConversionOption.SINGLEPDF);
				session.put(mainDocumentSessionKey, result);

			} else if ("convert".equalsIgnoreCase(action)) {
				String fileName = session.getMessage("fileName").asString();
				CisConversionResult cisConversionResult = null;
				CisConversionService cisConversionService = new CisConversionServiceImpl(pdfOutputLocation, fontManager.getFontsPath());
				cisConversionResult = cisConversionService.convertToPdf(binaryInputStream, fileName, saveSeparate ? ConversionOption.SEPERATEPDF : ConversionOption.SINGLEPDF);
				XmlBuilder main = new XmlBuilder("main");
				cisConversionResult.buildXmlFromResult(main, true);

				session.put("documents", main.toXML());
			}
			//TODO result should be converted document
			return new PipeRunResult(getSuccessForward(), "");
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot convert to stream",e);
		}
	}

	@IbisDoc({ "action to be processed by pdf pipe possible values:{combine, convert}", "null" })
	public void setAction(String action) { //TODO should be enum
		this.action = action;
	}
	public String getAction() {
		return action;
	}

	@IbisDoc({ "session key that contains the document that the attachments will be attached to. Only used when action is set to 'combine'", "defaultMainDocumentSessionKey" })
	public void setMainDocumentSessionKey(String mainDocumentSessionKey) {
		this.mainDocumentSessionKey = mainDocumentSessionKey;
	}
	public String getMainDocumentSessionKey() {
		return mainDocumentSessionKey;
	}

	@Deprecated
	@ConfigurationWarning("attribute 'fileNameToAttachSessionKey' is replaced with 'filenameToAttachSessionKey'")
	public void setFileNameToAttachSessionKey(String fileNameToAttachSessionKey) {
		this.filenameToAttachSessionKey = fileNameToAttachSessionKey;
	}

	@IbisDoc({ "session key that contains the filename to be attached. Only used when the action is set to 'combine' ", "defaultFileNameToAttachSessionKey" })
	public void setFilenameToAttachSessionKey(String filenameToAttachSessionKey) {
		this.filenameToAttachSessionKey = filenameToAttachSessionKey;
	}
	public String getFilenameToAttachSessionKey() {
		return filenameToAttachSessionKey;
	}

	@IbisDoc({ "fonts folder to load the fonts. If not set then a temporary folder will be created to extract fonts from fonts.zip everytime. Having fontsDirectory to be set will improve startup time", "null" })
	public void setFontsDirectory(String fontsDirectory) {
		this.fontsDirectory = fontsDirectory;
	}
	public String getFontsDirectory() {
		return fontsDirectory;
	}

	public void setUnpackCommonFontsArchive(boolean unpackDefaultFonts) {
		this.unpackDefaultFonts = unpackDefaultFonts;
	}

	@IbisDoc({ "charset to be used to encode the given input string", "UTF-8" })
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@IbisDoc({ "aspose license location including the file name. It can also be used without license but there some restrictions on usage. If license is in resource, license attribute can be license file name. If the license is in somewhere in filesystem then it should be full path to file including filename and starting with file://// prefix. classloader.allowed.protocols property should contain 'file' protocol", "" })
	public void setLicense(String license) {
		this.license = license;
	}
	public String getLicense() {
		return license;
	}

	@IbisDoc({ "when sets to false, converts the file including the attachments attached to the main file. when it is true, saves each attachment separately", "false" })
	public void setSaveSeparate(boolean saveSeparate) {
		this.saveSeparate = saveSeparate;
	}
	public boolean isSaveSeparate() {
		return saveSeparate;
	}

	@IbisDoc({ "directory to save resulting pdf files after conversion. If not set then a temporary directory will be created and the conversion results will be stored in that directory.", "null" })
	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputLocation = pdfOutputLocation;
	}
	public String getPdfOutputLocation() {
		return pdfOutputLocation;
	}
}
