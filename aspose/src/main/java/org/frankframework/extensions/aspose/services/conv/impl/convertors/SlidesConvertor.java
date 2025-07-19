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

import org.springframework.http.MediaType;

import com.aspose.slides.InvalidPasswordException;
import com.aspose.slides.LoadOptions;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionResult;
import org.frankframework.stream.Message;
import org.frankframework.util.ClassUtils;

/**
 * Converts the files which are required and supported by the aspose slides
 * library.
 *
 */
@Log4j2
public class SlidesConvertor extends AbstractConvertor {

	private static final Map<MediaType, Class<? extends LoadOptions>> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, Class<? extends LoadOptions>> map = new HashMap<>();

		map.put(new MediaType("application", "vnd.ms-powerpoint"), LoadOptions.class);
		map.put(new MediaType("application", "vnd.openxmlformats-officedocument.presentationml.presentation"), LoadOptions.class);
		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	protected SlidesConvertor(CisConfiguration configuration) {
		super(configuration, MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet());
	}

	@Override
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {
		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}
		try (InputStream inputStream = message.asInputStream(charset)) {
			LoadOptions loadOptions = getLoadOptions(mediaType);
			if(!configuration.isLoadExternalResources()){
				loadOptions.setResourceLoadingCallback(new OfflineResourceLoader());
			}
			Presentation presentation = new Presentation(inputStream, loadOptions);
			long startTime = System.currentTimeMillis();
			presentation.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.Pdf);
			long endTime = System.currentTimeMillis();
			log.debug("conversion (save operation in convert method) took [{}ms]", (endTime - startTime));
			presentation.dispose();
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
		}
	}

	private static LoadOptions getLoadOptions(final MediaType mediaType) throws Exception {
		return ClassUtils.newInstance(MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
