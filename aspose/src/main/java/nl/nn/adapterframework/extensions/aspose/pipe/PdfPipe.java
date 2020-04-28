/*
   Copyright 2019, 2020 Integration Partners

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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
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
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Converts files to pdf type. This pipe has two actions convert and combine. 
 * With combine action you can attach files into main pdf file. 
 * @author M64D844
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
			ConfigurationWarnings.add(this, log, "Aspose License is not configured. There will be evaluation watermarks on the converted documents. There are also some restrictions in the API use. License field could be set with a valid information to avoid this. ");
		}else {
			if(ClassUtils.getResourceURL(this, license) == null) {
				throw new ConfigurationException("Specified file for aspose license is not found");
			}
		}
		// load license 
		try {
			loader = new AsposeLicenseLoader(license, fontsDirectory);
			loader.loadLicense();
		} catch (Exception e) {
			throw new ConfigurationException("Error occured while loading the license", e);
		}
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		if (StringUtils.isEmpty(pdfOutputLocation)) {
			try {
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
				Files.delete(Paths.get(pdfOutputLocation));
				log.info("Temporary directory is deleted : " + pdfOutputLocation);
				pdfOutputLocation="";
			} catch (IOException e) {
				log.debug("Could not delete the temp folder " + pdfOutputLocation);
			}
		}
		super.stop();
	}

	@Override
	public PipeRunResult doPipe(Message input, IPipeLineSession session) throws PipeRunException {
		
		try (InputStream binaryInputStream = input.asInputStream(charset)) {

			if ("combine".equalsIgnoreCase(action)) {
				// Get main document to attach attachments
				InputStream mainPdf = Message.asInputStream(session.get(mainDocumentSessionKey),charset);
				// Get file name of attachment
				String fileNameToAttach = Message.asString(session.get(fileNameToAttachSessionKey));
	
				InputStream result = PdfAttachmentUtil.combineFiles(mainPdf, binaryInputStream, fileNameToAttach + ".pdf");
	
				session.put("CONVERSION_OPTION", ConversionOption.SINGLEPDF);
				session.put(mainDocumentSessionKey, result);
	
			} else if ("convert".equalsIgnoreCase(action)) {
				String fileName = (String) session.get("fileName");
				CisConversionResult cisConversionResult = null;
				CisConversionService cisConversionService = new CisConversionServiceImpl(pdfOutputLocation, loader.getPathToExtractFonts());
				cisConversionResult = cisConversionService.convertToPdf(binaryInputStream, fileName, saveSeparate ? ConversionOption.SEPERATEPDF : ConversionOption.SINGLEPDF);
				XmlBuilder main = new XmlBuilder("main");
				cisConversionResult.buildXmlFromResult(main, cisConversionResult, true);
				
				session.put("documents", main.toXML());
			}
			return new PipeRunResult(getForward(), "");
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot convert to stream",e);
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

	@IbisDoc({ "aspose license location including the file name. It can also be used without license but there some restrictions on usage. If license is in resource, license attribute can be license file name. If the license is in somewhere in filesystem then it should be full path to file including filename and starting with file://// prefix. classloader.allowed.protocols property should contain 'file' protocol", "" })
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
