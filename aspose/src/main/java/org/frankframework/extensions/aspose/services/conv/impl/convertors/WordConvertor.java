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

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.annotation.Nullable;

import org.springframework.http.MediaType;

import com.aspose.words.Document;
import com.aspose.words.HtmlLoadOptions;
import com.aspose.words.IncorrectPasswordException;
import com.aspose.words.LoadFormat;
import com.aspose.words.LoadOptions;
import com.aspose.words.SaveFormat;
import com.aspose.words.SaveOptions;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.stream.Message;

/**
 * Converts the files which are required and supported by the aspose words
 * library.
 *
 */
@Log4j2
class WordConvertor extends AbstractConvertor {
	private static final Map<MediaType, Supplier<LoadOptions>> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, Supplier<LoadOptions>> map = new HashMap<>();

		// Mapping to loadOptions
		map.put(new MediaType("application", "msword"), null);
		map.put(new MediaType("application", "vnd.openxmlformats-officedocument.wordprocessingml.document"), null);
		map.put(new MediaType("application", "vnd.ms-word.document.macroenabled.12"), null);
		map.put(new MediaType("application", "x-tika-msoffice"), null);

		// The string value is defined in com.aspose.words.LoadFormat.
		map.put(new MediaType("text", "plain"), ()->new LoadOptions(LoadFormat.fromName("TEXT"), null, null));
		map.put(new MediaType("text", "x-log"), ()->new LoadOptions(LoadFormat.fromName("TEXT"), null, null));
		map.put(new MediaType("text", "csv"), ()->new LoadOptions(LoadFormat.fromName("TEXT"), null, null));

		// The string value is defined in com.aspose.words.LoadFormat.
		map.put(new MediaType("application", "rtf"), ()->new LoadOptions(LoadFormat.fromName("RTF"), null, null));

		map.put(new MediaType("application", "xml"), ()->new LoadOptions(LoadFormat.fromName("TEXT"), null, null));
		map.put(new MediaType("text", "html"), HtmlLoadOptions::new);
		map.put(new MediaType("application", "xhtml+xml"), HtmlLoadOptions::new);
		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	protected WordConvertor(CisConfiguration cisConfiguration) {
		super(cisConfiguration, MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet());
	}

	@Override
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {

		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}

		try (InputStream inputStream = message.asInputStream(charset)) {
			LoadOptions loadOptions = getLoadOptions(mediaType);

			Document doc = new Document(inputStream, loadOptions);
			new FontManager(configuration.getFontsDirectory()).setFontSettings(doc);
			SaveOptions saveOptions = SaveOptions.createSaveOptions(SaveFormat.PDF);
			saveOptions.setMemoryOptimization(true);

			long startTime = System.currentTimeMillis();
			doc.save(result.getPdfResultFile().getAbsolutePath(), saveOptions);
			long endTime = System.currentTimeMillis();
			log.debug("conversion (save operation in convert method) took [{}ms]", (endTime - startTime));
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
		}
	}

	@Nullable
	private LoadOptions getLoadOptions(final MediaType mediaType) {
		Supplier<LoadOptions> loadOptionsSupplier = MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType);
		if (loadOptionsSupplier == null) {
			return null;
		}
		LoadOptions loadOptions = loadOptionsSupplier.get();
		if(!configuration.isLoadExternalResources()){
			loadOptions.setResourceLoadingCallback(new OfflineResourceLoader());
		}
		return loadOptions;
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof IncorrectPasswordException;
	}

}
