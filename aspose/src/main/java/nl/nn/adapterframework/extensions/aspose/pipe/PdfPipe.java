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
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.extensions.aspose.AsposeFontManager;
import nl.nn.adapterframework.extensions.aspose.AsposeLicenseLoader;
import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConfiguration;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.CisConversionServiceImpl;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.PdfAttachmentUtil;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.XmlBuilder;


/**
 * Converts files to pdf type. This pipe has two actions convert and combine.
 * With combine action you can attach files into main pdf file.
 *
 */
public class PdfPipe extends FixedForwardPipe {

	private static final String FILENAME_SESSION_KEY = "fileName";

	private @Getter boolean saveSeparate = false;
	private @Getter String pdfOutputLocation = null;
	private @Getter String fontsDirectory;
	private @Getter String license = null;
	private @Getter DocumentAction action = null;
	private @Getter String mainDocumentSessionKey = "defaultMainDocumentSessionKey";
	private @Getter String filenameToAttachSessionKey = "defaultFileNameToAttachSessionKey";
	private @Getter String charset = null;
	private AsposeFontManager fontManager;
	private @Getter boolean unpackDefaultFonts = false;
	private @Getter boolean loadExternalResources = false;

	private CisConversionService cisConversionService;

	protected enum DocumentAction {
		CONVERT,
		COMBINE;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if(getAction() == null) {
			throw new ConfigurationException("please specify an action for pdf pipe ["+getName()+"]. possible values: "+EnumUtils.getEnumList(DocumentAction.class));
		}
		if(StringUtils.isNotEmpty(getPdfOutputLocation())) {
			File outputLocation = new File(getPdfOutputLocation());
			if (!outputLocation.exists()) {
				throw new ConfigurationException("pdf output location does not exist");
			}

			if (!outputLocation.isDirectory()) {
				throw new ConfigurationException("pdf output location is not a valid directory");
			}
		} else {
			try {
				String ibisTempDir = FileUtils.getTempDirectory();
				if(StringUtils.isNotEmpty(ibisTempDir)) {
					setPdfOutputLocation(Files.createTempDirectory(Paths.get(ibisTempDir),"Pdf").toString());
				} else {
					setPdfOutputLocation(Files.createTempDirectory("Pdf").toString());
				}
				log.info("Temporary directory path : " + getPdfOutputLocation());
			} catch (IOException e) {
				throw new ConfigurationException(e);
			}
		}
		if (StringUtils.isEmpty(getLicense())) {
			ConfigurationWarnings.add(this, log, "Aspose License is not configured. There will be evaluation watermarks on the converted documents. There are also some restrictions in the API use. License field should be set with a valid information to avoid this. ");
		} else {
			URL licenseUrl = ClassUtils.getResourceURL(getLicense());
			if(licenseUrl == null) {
				throw new ConfigurationException("specified file for aspose license is not found");
			}

			try {
				AsposeLicenseLoader.loadLicenses(licenseUrl);
			} catch (Exception e) {
				throw new ConfigurationException("an error occured while loading Aspose license(s)");
			}
		}

		fontManager = new AsposeFontManager(getFontsDirectory());
		try {
			fontManager.load(isUnpackDefaultFonts());
		} catch (IOException e) {
			throw new ConfigurationException("an error occured while loading fonts", e);
		}

		CisConfiguration configuration = new CisConfiguration(loadExternalResources, getPdfOutputLocation(), getCharset(), fontManager.getFontsPath());
		cisConversionService = new CisConversionServiceImpl(configuration);
	}

	@Override
	public PipeRunResult doPipe(Message input, PipeLineSession session) throws PipeRunException {
		// message should always be available.
		if (Message.isEmpty(input)) {
			throw new IllegalArgumentException("message == null");
		}
		try {
			switch(getAction()) {
				case COMBINE:
					// Get main document to attach attachments
					Message mainPdf = session.getMessage(getMainDocumentSessionKey());
					// Get file name of attachment
					String fileNameToAttach = session.getMessage(getFilenameToAttachSessionKey()).asString();

					Message result = PdfAttachmentUtil.combineFiles(mainPdf, input, fileNameToAttach + ".pdf", getCharset());

					session.put("CONVERSION_OPTION", ConversionOption.SINGLEPDF);
					session.put(getMainDocumentSessionKey(), result);
					return new PipeRunResult(getSuccessForward(), result);
				case CONVERT:
					String filename = session.getMessage(FILENAME_SESSION_KEY).asString();
					CisConversionResult cisConversionResult = cisConversionService.convertToPdf(input, filename, isSaveSeparate() ? ConversionOption.SEPARATEPDF : ConversionOption.SINGLEPDF);
					XmlBuilder main = new XmlBuilder("main");
					cisConversionResult.buildXmlFromResult(main, true);

					session.put("documents", main.toXML());
					return new PipeRunResult(getSuccessForward(), main.toXML());
				default:
					throw new PipeRunException(this, "action attribute must be one of the followings: "+EnumUtils.getEnumList(DocumentAction.class));
			}
		} catch (IOException e) {
			throw new PipeRunException(this, getLogPrefix(session)+"cannot convert to stream",e);
		}
	}

	public void setAction(DocumentAction action) {
		this.action = action;
	}

	@IbisDoc({ "session key that contains the document that the attachments will be attached to. Only used when action is set to 'combine'", "defaultMainDocumentSessionKey" })
	public void setMainDocumentSessionKey(String mainDocumentSessionKey) {
		this.mainDocumentSessionKey = mainDocumentSessionKey;
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

	@IbisDoc({ "fonts folder to load the fonts. If not set then a temporary folder will be created to extract fonts from fonts.zip everytime. Having fontsDirectory to be set will improve startup time", "null" })
	public void setFontsDirectory(String fontsDirectory) {
		this.fontsDirectory = fontsDirectory;
	}

	public void setUnpackCommonFontsArchive(boolean unpackDefaultFonts) {
		this.unpackDefaultFonts = unpackDefaultFonts;
	}

	@IbisDoc({ "charset to be used to decode the given input message in case the input is not binary but character stream", "UTF-8" })
	public void setCharset(String charset) {
		this.charset = charset;
	}

	@IbisDoc({ "aspose license location including the file name. It can also be used without license but there some restrictions on usage. If license is in resource, license attribute can be license file name. If the license is in somewhere in filesystem then it should be full path to file including filename and starting with file://// prefix. classloader.allowed.protocols property should contain 'file' protocol", "" })
	public void setLicense(String license) {
		this.license = license;
	}

	@IbisDoc({ "when sets to false, converts the file including the attachments attached to the main file. when it is true, saves each attachment separately", "false" })
	public void setSaveSeparate(boolean saveSeparate) {
		this.saveSeparate = saveSeparate;
	}

	@IbisDoc({ "directory to save resulting pdf files after conversion. If not set then a temporary directory will be created and the conversion results will be stored in that directory.", "null" })
	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputLocation = pdfOutputLocation;
	}

	@IbisDoc({ "when set to true, external resources, such as stylesheets and images found in HTML pages, will be loaded from the internet", "false" })
	public void setLoadExternalResources(boolean loadExternalResources) {
		this.loadExternalResources = loadExternalResources;
	}
}
