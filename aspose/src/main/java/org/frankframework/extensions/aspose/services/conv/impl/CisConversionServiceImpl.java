/*
   Copyright 2019, 2021-2025 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv.impl;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.ConversionOption;
import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionException;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.extensions.aspose.services.conv.CisConversionService;
import org.frankframework.extensions.aspose.services.conv.impl.convertors.Convertor;
import org.frankframework.extensions.aspose.services.conv.impl.convertors.ConvertorFactory;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;
/**
 * @author Gerard van der Hoorn
 */
@Log4j2
public class CisConversionServiceImpl implements CisConversionService {
	private final CisConfiguration configuration;
	private final ConvertorFactory convertorFactory;
	private final MediaTypeValidator mediaTypeValidator = new MediaTypeValidator();

	public CisConversionServiceImpl(CisConfiguration configuration){
		this.configuration = configuration;
		convertorFactory = new ConvertorFactory(this, configuration);
	}

	@Override
	public CisConversionResult convertToPdf(Message message, String filename, ConversionOption conversionOption) {

		CisConversionResult result = null;
		MimeType mimeType = MessageUtils.computeMimeType(message, filename);
		if(mimeType == null || "x-tika-msoffice".equals(mimeType.getSubtype())) {
			// MessageUtils.computeMimeType may return the mimetype already set on the message, for instance from request header.
			// For TIKA MS Office files we need to enforce that TIKA actually checks the message contents.
			// MS Office files can be password protected, which can only be determined by reading a part of the file.
			mimeType = getMediaType(message, filename);
		}

		MediaType mediaType = new MediaType(mimeType.getType(), mimeType.getSubtype()); //Strip all parameters
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
				result = convertor.convertToPdf(mediaType, filename, message, conversionOption, configuration.getCharset());
				if(log.isDebugEnabled()) log.debug("Convert (in %d msec): mediatype: %s, filename: %s, attachmentoptions: %s".formatted(System.currentTimeMillis() - startTime, mediaType, filename, conversionOption));
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
		msg.append(" ").append(message);

		log.warn("Conversion not supported: {}", msg);

		result = CisConversionResult.createFailureResult(conversionOption, mediaType, filename, msg.toString());
		return result;
	}

	private boolean isPasswordProtected(MediaType mediaType) {
		return "x-tika-ooxml-protected".equals(mediaType.getSubtype());
	}

	/**
	 * Read the message to determine the MediaType
	 */
	private MediaType getMediaType(Message message, String filename) {
		MediaType mediaType = null;
		try {
			mediaType = mediaTypeValidator.getMediaType(message, filename);
			log.debug("detected mediatype [{}]",mediaType);
		} catch (IOException e) {
			throw new CisConversionException("Het omzetten naar pdf is mislukt. Neem contact op met de functioneel beheerder", e);
		}

		return mediaType;
	}
}
