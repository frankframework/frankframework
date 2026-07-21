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
package org.frankframework.extensions.aspose.converters;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.util.MimeType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.CisConfiguration;
import org.frankframework.extensions.aspose.services.CisConversionResult;
import org.frankframework.extensions.aspose.services.InvalidPasswordException;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

@Log4j2
abstract class AbstractConverter implements Converter {
	protected static final MimeType PDF_MIMETYPE = new MediaType("application", "pdf");

	private final Set<MediaType> supportedMediaTypes;
	protected CisConfiguration configuration;

	protected AbstractConverter(CisConfiguration configuration, Set<MediaType> mediaTypes) {
		this.configuration = configuration;
		this.supportedMediaTypes = Collections.unmodifiableSet(mediaTypes);
	}

	protected AbstractConverter(CisConfiguration configuration, MediaType... args) {
		this(configuration, new HashSet<>(Arrays.asList(args)));
	}

	/**
	 * Returns the supported media types.
	 */
	@Override
	public Set<MediaType> getSupportedMediaTypes() {
		return supportedMediaTypes;
	}

	private void checkForSupportedMediaType(MediaType mediaType) {
		boolean supported = getSupportedMediaTypes().contains(mediaType);

		// Create informative message.
		if (!supported) {
			StringBuilder builder = new StringBuilder();
			builder.append("This convertor '");
			builder.append(this.getClass().getSimpleName());
			builder.append("' only supports ");

			boolean first = true;
			for (MediaType mediaTypeSupported : getSupportedMediaTypes()) {
				if (!first) {
					builder.append(" or");
				}
				first = false;
				builder.append(" '");
				builder.append(mediaTypeSupported.toString());
				builder.append("'");
			}
			builder.append("! (received '");
			builder.append(mediaType);
			builder.append("')");
			throw new IllegalArgumentException(builder.toString());
		}
	}

	/**
	 * Should not be overloaded by the concrete classes.
	 */
	@Override
	public void convertToPdf(CisConversionResult result, MediaType mediaType, Message message) throws Exception {
		checkForSupportedMediaType(mediaType);

		try {
			log.debug("converting [{}] to PDF using converter [{}]", () -> message, () -> ClassUtils.nameOf(this));
			result.setMessage(convert(mediaType, message));
			log.trace("converting [{}] to file finished", message);
		} catch (Exception e) {
			if (isPasswordException(e)) {
				throw new InvalidPasswordException(e);
			} else {
				throw e;
			}
		}
	}

	/**
	 * Converts the given file to a PDF. MediaType is the detected media type of the
	 * file. The convertor should support the given mediaType (otherwise it is a
	 * programming error).
	 */
	@SuppressWarnings("java:S112") // generic exceptions
	protected abstract Message convert(MediaType mediaType, Message file) throws Exception;

	protected abstract boolean isPasswordException(Exception e);
}
