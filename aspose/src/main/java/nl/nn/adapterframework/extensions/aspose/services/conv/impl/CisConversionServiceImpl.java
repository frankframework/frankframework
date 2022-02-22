/*
   Copyright 2019, 2021-2022 WeAreFrank!

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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl;

import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionException;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.Convertor;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.ConvertorFactory;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageUtils;
/**
 * @author Gerard van der Hoorn
 */
public class CisConversionServiceImpl implements CisConversionService {

	private static final Logger LOGGER = LogUtil.getLogger(CisConversionServiceImpl.class);

	private String pdfOutputlocation = "";
	private ConvertorFactory convertorFactory;
	private MediaTypeValidator mediaTypeValidator;
	private String fontsDirectory;
	private String charset;

	public CisConversionServiceImpl(String pdfOutputLocation, String fontsDirectory, String charset) {
		this.pdfOutputlocation = pdfOutputLocation;
		this.setFontsDirectory(fontsDirectory);
		convertorFactory = new ConvertorFactory(this, pdfOutputlocation);
		mediaTypeValidator = new MediaTypeValidator();
		this.charset=charset;
	}

	@Override
	public CisConversionResult convertToPdf(Message message, String filename, ConversionOption conversionOption) throws IOException {

		CisConversionResult result = null;
		MimeType mimeType = MessageUtils.computeMimeType(message, filename);
		if(mimeType == null || "x-tika-msoffice".equals(mimeType.getSubtype())) {
			// If we cannot determine the MimeType based on the files magic numbers, read part of the file.
			// MS Office files can be password protected, which can only be determined by reading a part of the file.
			mimeType = getMediaType(message, filename);
		}

		MediaType mediaType = MediaType.asMediaType(mimeType);
		if (isPasswordProtected(mediaType)) {
			result = CisConversionResult.createPasswordFailureResult(filename, conversionOption, mediaType);
		} else {
			// Get the convertor for the given mediatype.
			Convertor convertor = convertorFactory.getConvertor(mediaType);
			if (convertor == null) {
				// Conversion not supported.
				String errorMessage = "Omzetten naar PDF mislukt! Reden: bestandstype wordt niet ondersteund (mediaType: "+ mediaType + ")";
				result = createFailureResult(filename, conversionOption, mediaType, errorMessage);
			} else {
				long startTime = System.currentTimeMillis();
				// Convertor found, convert the file
				result = convertor.convertToPdf(mediaType, filename, message, conversionOption, charset);
				if(LOGGER.isDebugEnabled()) LOGGER.debug(String.format("Convert (in %d msec): mediatype: %s, filename: %s, attachmentoptions: %s", System.currentTimeMillis() - startTime, mediaType, filename, conversionOption));
			}
		}
		return result;
	}

	private CisConversionResult createFailureResult(String filename, ConversionOption conversionOption, MediaType mediaType, String message) {
		CisConversionResult result;
		StringBuilder msg = new StringBuilder();
		if (filename != null) {
			msg.append(filename);
		}
		msg.append(" " + message);

		LOGGER.warn("Conversion not supported: " + msg.toString());

		result = CisConversionResult.createFailureResult(conversionOption, mediaType, filename, msg.toString());
		return result;
	}

	private boolean isPasswordProtected(MediaType mediaType) {
		return ("x-tika-ooxml-protected".equals(mediaType.getSubtype()));
	}

	/**
	 * Read the message to determine the MediaType
	 */
	private MediaType getMediaType(Message message, String filename) {
		MediaType mediaType = null;
		try {
			mediaType = mediaTypeValidator.getMediaType(message, filename);
			LOGGER.debug("Mediatype received: " + mediaType);
		} catch (IOException e) {
			throw new CisConversionException("Het omzetten naar pdf is mislukt. Neem contact op met de functioneel beheerder", e);
		}

		return mediaType;
	}

	public void setPdfOutputLocation(String pdfOutputLocation) {
		this.pdfOutputlocation = pdfOutputLocation;
	}

	@Override
	public String getFontsDirectory() {
		return fontsDirectory;
	}

	public void setFontsDirectory(String fontsDirectory) {
		this.fontsDirectory = fontsDirectory;
	}

}
