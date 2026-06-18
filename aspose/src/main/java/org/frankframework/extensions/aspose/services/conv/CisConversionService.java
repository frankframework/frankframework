/*
   Copyright 2019-2026 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv;

import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.impl.convertors.Convertor;
import org.frankframework.extensions.aspose.services.conv.impl.convertors.ConvertorFactory;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.MessageUtils;

/**
 * @author Gerard van der Hoorn
 */
@Log4j2
public class CisConversionService {
	private final ConvertorFactory convertorFactory;

	public CisConversionService(CisConfiguration configuration) {
		convertorFactory = new ConvertorFactory(configuration);
	}

	public CisConversionResult convertToPdf(Message message) {
		String filename = (String) message.getContext().get(MessageContext.METADATA_NAME);
		MediaType mediaType = getMimeType(message, filename);

		if (isPasswordProtected(mediaType)) {
			return CisConversionResult.createPasswordFailureResult(filename, mediaType);
		} else {
			// Get the converter for the given mediatype.
			Convertor convertor = convertorFactory.getConvertor(mediaType);
			if (convertor == null) {
				// Conversion not supported.
				String errorMessage = "Omzetten naar PDF mislukt! Reden: bestandstype wordt niet ondersteund (mediaType: "+ mediaType + ")";
				return CisConversionResult.createFailureResult(mediaType, filename, errorMessage);
			} else {
				CisConversionResult result = new CisConversionResult();
				result.setMediaType(mediaType);
				result.setDocumentName(filename);

				long startTime = System.currentTimeMillis();
				// Convertor found, convert the file
				try {
					convertor.convertToPdf(result, mediaType, message);
					log.debug("Convert (in {} msec): mediatype: {}, filename: {}", System.currentTimeMillis() - startTime, mediaType, filename);
					return result;
				} catch (InvalidPasswordException e) {
					return CisConversionResult.createPasswordFailureResult(filename, mediaType);
				} catch (Exception e) {
					return CisConversionResult.createFailureResult(mediaType, filename, e.getMessage());
				}
			}
		}
	}

	private boolean isPasswordProtected(MediaType mediaType) {
		return "x-tika-ooxml-protected".equals(mediaType.getSubtype());
	}

	/**
	 * Read the message to determine the MediaType.
	 * MessageUtils.computeMimeType may return the mimetype already set on the message, for instance from request header.
	 * For TIKA MS Office files we need to enforce that TIKA actually checks the message contents.
	 * MS Office files can be password protected, which can only be determined by reading a part of the file.
	 */
	private MediaType getMimeType(Message message, String filename) {
		// MessageUtils.computeMimeType may return the mimetype already set on the message, for instance from request header.
		// For TIKA MS Office files we need to enforce that TIKA actually checks the message contents.
		if (MessageUtils.getMimeType(message) != null && "x-tika-msoffice".equals(MessageUtils.getMimeType(message).getSubtype())) {
			message.getContext().put(MessageContext.METADATA_MIMETYPE, null);
		}

		MimeType mimeType = MessageUtils.computeMimeType(message, filename);
		if (mimeType == null) {
			throw new IllegalStateException("Het omzetten naar pdf is mislukt. Neem contact op met de functioneel beheerder");
		}

		return new MediaType(mimeType.getType(), mimeType.getSubtype()); // Strip all parameters
	}
}
