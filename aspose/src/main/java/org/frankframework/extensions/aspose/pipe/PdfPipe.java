/*
   Copyright 2019 - 2024 WeAreFrank!

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
package org.frankframework.extensions.aspose.pipe;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.extensions.aspose.AsposeFontManager;
import org.frankframework.extensions.aspose.AsposeLicenseLoader;
import org.frankframework.extensions.aspose.ConversionOption;
import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.extensions.aspose.services.conv.CisConversionService;
import org.frankframework.extensions.aspose.services.conv.impl.CisConversionServiceImpl;
import org.frankframework.extensions.aspose.services.conv.impl.convertors.PdfAttachmentUtil;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.TemporaryDirectoryUtils;
import org.frankframework.util.XmlBuilder;


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
	private @Getter String conversionResultDocumentSessionKey = "documents";
	private @Getter String conversionResultFilesSessionKey = "pdfConversionResultFiles";
	private @Getter String charset = null;
	private @Getter boolean unpackDefaultFonts = false;
	private @Getter boolean loadExternalResources = false;

	private CisConversionService cisConversionService;

	public enum DocumentAction {
		CONVERT,
		COMBINE
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if(getAction() == null) {
			throw new ConfigurationException("please specify an action for pdf pipe ["+getName()+"]. possible values: "+ EnumUtils.getEnumList(DocumentAction.class));
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
				Path ibisTempDir = TemporaryDirectoryUtils.getTempDirectory("Pdf");
				setPdfOutputLocation(ibisTempDir.toString());
				log.info("temporary directory path [{}]", ibisTempDir);
			} catch (IOException e) {
				throw new ConfigurationException(e);
			}
		}
		if (StringUtils.isEmpty(getLicense())) {
			ConfigurationWarnings.add(this, log, "Aspose License is not configured. There will be evaluation watermarks on the converted documents. There are also some restrictions in the API use. License field should be set with a valid information to avoid this. ");
		} else {
			URL licenseUrl = ClassLoaderUtils.getResourceURL(getLicense());
			if(licenseUrl == null) {
				throw new ConfigurationException("specified file for aspose license is not found");
			}

			try {
				AsposeLicenseLoader.loadLicenses(licenseUrl);
			} catch (Exception e) {
				throw new ConfigurationException("an error occurred while loading Aspose license(s)");
			}
		}

		AsposeFontManager fontManager;
		try {
			fontManager = new AsposeFontManager(getFontsDirectory());
			fontManager.load(isUnpackDefaultFonts());
		} catch (IOException e) {
			throw new ConfigurationException("an error occurred while loading fonts", e);
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
					String fileNameToAttach = session.getString(getFilenameToAttachSessionKey());

					Message result = PdfAttachmentUtil.combineFiles(mainPdf, input, fileNameToAttach + ".pdf", getCharset());

					session.put("CONVERSION_OPTION", ConversionOption.SINGLEPDF);
					session.put(getMainDocumentSessionKey(), result);
					return new PipeRunResult(getSuccessForward(), result);
				case CONVERT:
					String filename = session.getString(FILENAME_SESSION_KEY);
					CisConversionResult cisConversionResult = cisConversionService.convertToPdf(input, filename, isSaveSeparate() ? ConversionOption.SEPARATEPDF : ConversionOption.SINGLEPDF);

					// Populate Session before creating main-xml as it will update session keys.
					populateSession(cisConversionResult, session, new MutableInt(0));

					XmlBuilder main = new XmlBuilder("main");
					cisConversionResult.buildXmlFromResult(main, true);

					Message message = main.asMessage();
					session.put(getConversionResultDocumentSessionKey(), message);

					return new PipeRunResult(getSuccessForward(), message);
				default:
					throw new PipeRunException(this, "action attribute must be one of the followings: "+EnumUtils.getEnumList(DocumentAction.class));
			}
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot convert to stream",e);
		}
	}

	private void populateSession(CisConversionResult result, PipeLineSession session, MutableInt index) {
		if (StringUtils.isNotEmpty(result.getResultFilePath())) {
			// TODO: Use a PathMessage.asTemporaryMessage() here in future so that all these files
			//       are automatically cleaned up on close of the PipeLineSession.
			FileMessage document = new FileMessage(new File(result.getResultFilePath()));
			String sessionKey = getConversionResultFilesSessionKey() + index.incrementAndGet();
			result.setResultSessionKey(sessionKey);
			session.put(sessionKey, document);
		}

		List<CisConversionResult> attachmentList = result.getAttachments();
		for (CisConversionResult cisConversionResult : attachmentList) {
			populateSession(cisConversionResult, session, index);
		}
	}

	public void setAction(DocumentAction action) {
		this.action = action;
	}

	/**
	 * session key that contains the document that the attachments will be attached to. Only used when action is set to 'combine'
	 * @ff.default defaultMainDocumentSessionKey
	 */
	public void setMainDocumentSessionKey(String mainDocumentSessionKey) {
		this.mainDocumentSessionKey = mainDocumentSessionKey;
	}

	/**
	 * The session key used to store the main conversion result document. Only to be used when action is set to 'convert'.
	 *
	 * @param conversionResultDocumentSessionKey Name of the session key.
	 * @ff.default documents
	 */
	public void setConversionResultDocumentSessionKey(String conversionResultDocumentSessionKey) {
		this.conversionResultDocumentSessionKey = conversionResultDocumentSessionKey;
	}

	/**
	 * The session-key in which result files are stored when documents are converted to PDF.
	 *
	 * <p>
	 * Conversion result files are stored as messages in the session, under keys numbered based
	 * on the value set here. If {@link #isSaveSeparate()} is {@code false} then only the main
	 * document is stored in the session, if it is {@code true} then each attachment is stored
	 * separately.
	 * </p>
	 * <p>
	 *     For example, if a file is converted that has 2 attachments and {@link #setSaveSeparate(boolean)}
	 *     is set to {@code true} then there will be the following 3 session keys (assuming the default value
	 *     is unchanged):
	 *     <ol>
	 *         <li>{@code pdfConversionResultFiles1}</li>
	 *         <li>{@code pdfConversionResultFiles2}</li>
	 *         <li>{@code pdfConversionResultFiles3}</li>
	 *     </ol>
	 *     Each session key will contain a {@link FileMessage} referencing the contents of that PDF.
	 * </p>
	 * @ff.default pdfConversionResultFiles
	 *
	 * @param conversionResultFilesSessionKey The name of the session key under which PDF documents are stored.
	 */
	public void setConversionResultFilesSessionKey(String conversionResultFilesSessionKey) {
		this.conversionResultFilesSessionKey = conversionResultFilesSessionKey;
	}

	/**
	 * session key that contains the filename to be attached. Only used when the action is set to 'combine'
	 * @ff.default defaultFileNameToAttachSessionKey
	 */
	public void setFilenameToAttachSessionKey(String filenameToAttachSessionKey) {
		this.filenameToAttachSessionKey = filenameToAttachSessionKey;
	}

	/**
	 * fonts folder to load the fonts. If not set then a temporary folder will be created to extract fonts from fonts.zip everytime. Having fontsDirectory to be set will improve startup time
	 * @ff.default null
	 */
	public void setFontsDirectory(String fontsDirectory) {
		this.fontsDirectory = fontsDirectory;
	}

	public void setUnpackCommonFontsArchive(boolean unpackDefaultFonts) {
		this.unpackDefaultFonts = unpackDefaultFonts;
	}

	/**
	 * charset to be used to decode the given input message in case the input is not binary but character stream
	 * @ff.default UTF-8
	 */
	@Deprecated(since = "9.3.0", forRemoval = true)
	@ConfigurationWarning("Charset property will be removed in a future version. ")
	public void setCharset(String charset) {
		this.charset = charset;
	}

	/** aspose license location including the file name. It can also be used without license but there some restrictions on usage. If license is in resource, license attribute can be license file name. If the license is in somewhere in filesystem then it should be full path to file including filename and starting with file://// prefix. classloader.allowed.protocols property should contain 'file' protocol */
	public void setLicense(String license) {
		this.license = license;
	}

	/**
	 * when sets to false, converts the file including the attachments attached to the main file. when it is true, saves each attachment separately
	 * @ff.default false
	 */
	public void setSaveSeparate(boolean saveSeparate) {
		this.saveSeparate = saveSeparate;
	}

	/**
	 * directory to save resulting pdf files after conversion. If not set then a temporary directory will be created and the conversion results will be stored in that directory.
	 * @ff.default null
	 */
	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputLocation = pdfOutputLocation;
	}

	/**
	 * when set to true, external resources, such as stylesheets and images found in HTML pages, will be loaded from the internet
	 * @ff.default false
	 */
	public void setLoadExternalResources(boolean loadExternalResources) {
		this.loadExternalResources = loadExternalResources;
	}
}
