/*
   Copyright 2019, 2021-2023 WeAreFrank!

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
package org.frankframework.extensions.aspose.services.conv.impl.convertors;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.springframework.http.MediaType;

import com.aspose.pdf.Document;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.ConversionOption;
import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.extensions.aspose.services.util.ConvertorUtil;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.DateFormatUtils;

@Log4j2
abstract class AbstractConvertor implements Convertor {

	private final Set<MediaType> supportedMediaTypes;
	protected CisConfiguration configuration;

	protected AbstractConvertor(CisConfiguration configuration, Set<MediaType> mediaTypes) {
		this.configuration = configuration;
		this.supportedMediaTypes = Collections.unmodifiableSet(mediaTypes);
	}

	protected AbstractConvertor(CisConfiguration configuration, MediaType... args) {
		this(configuration, new HashSet<>(Arrays.asList(args)));
	}

	protected abstract void convert(MediaType mediaType, Message file, CisConversionResult builder, String charset) throws Exception;

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
	public final CisConversionResult convertToPdf(MediaType mediaType, String filename, Message message, ConversionOption conversionOption, String charset) {

		checkForSupportedMediaType(mediaType);

		CisConversionResult result = new CisConversionResult();
		File resultFile;
		try {
			resultFile = UniqueFileGenerator.getUniqueFile(configuration.getPdfOutputLocation(), this.getClass().getSimpleName(), "pdf");
			result.setConversionOption(conversionOption);
			result.setMediaType(mediaType);
			result.setDocumentName(ConvertorUtil.createTidyNameWithoutExtension(filename));
			result.setPdfResultFile(resultFile);
			result.setResultFilePath(resultFile.getAbsolutePath());

			log.debug("Convert to file [{}]", filename);
			convert(mediaType, message, result, charset);
			log.debug("Convert to file finished. [{}]", filename);
		} catch (Exception e) {
			if (isPasswordException(e)) {
				result = CisConversionResult.createPasswordFailureResult(filename, conversionOption, mediaType);
			} else {
				result.setFailureReason(createErrorMsg(e));
			}
			// Clear the file to state that the conversion has failed.
			result.setPdfResultFile(null);
		}
		return result;
	}

	protected int getNumberOfPages(File file) throws IOException {
		if(file != null) {
			try (InputStream inStream = Files.newInputStream(file.toPath())) {
				try(Document doc = new Document(inStream)) {
					return doc.getPages().size();
				}
			}
		}

		return 0;
	}

	protected String createErrorMsg(Exception e) {
		String timestamp = DateFormatUtils.now();
		log.warn("failed to convert [{}] failed! (Timestamp: [{}])", () -> ClassUtils.classNameOf(this), () -> timestamp, () -> e);
		return "Conversion to PDF failed due to a technical failure. Please contact functional support." +
				"(Timestamp: " + timestamp + ")";
	}

	protected void deleteFile(File file) throws IOException {
		// Delete always the temporary file if it exists.
		if (file != null && Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			try {
				Files.delete(file.toPath());
			} catch (IOException e) {
				log.warn("failed to delete file [{}]", file, e);
				throw new IOException("Deleting file [" + file + "] failed!", e);
			}
		}
	}

	protected abstract boolean isPasswordException(Exception e);
}
