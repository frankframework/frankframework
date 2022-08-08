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
import org.apache.tika.mime.MediaType;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConfiguration;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionException;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.Convertor;
import nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors.ConvertorFactory;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
/**
 * @author Gerard van der Hoorn
 */
public class CisConversionServiceImpl implements CisConversionService {

	private static final Logger LOGGER = LogUtil.getLogger(CisConversionServiceImpl.class);

	private CisConfiguration configuration;
	private ConvertorFactory convertorFactory;
	private MediaTypeValidator mediaTypeValidator = new MediaTypeValidator();

	public CisConversionServiceImpl(CisConfiguration configuration){
		this.configuration = configuration;
		convertorFactory = new ConvertorFactory(this, configuration);
	}

	@Override
	public CisConversionResult convertToPdf(Message message, String filename, ConversionOption conversionOption) {

		CisConversionResult result = null;
		MediaType mediaType = getMediaType(message, filename);

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
}
