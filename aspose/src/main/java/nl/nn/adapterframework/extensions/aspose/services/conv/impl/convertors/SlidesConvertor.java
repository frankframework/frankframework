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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.extensions.aspose.services.conv.CisConfiguration;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;

import com.aspose.slides.InvalidPasswordException;
import com.aspose.slides.LoadOptions;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Converts the files which are required and supported by the aspose slides
 * library.
 *
 */
public class SlidesConvertor extends AbstractConvertor {

	private static final Logger LOGGER = LogUtil.getLogger(SlidesConvertor.class);
	private static final Map<MediaType, LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, LoadOptions> map = new HashMap<>();

		map.put(new MediaType("application", "vnd.ms-powerpoint"), new LoadOptions());
		map.put(new MediaType("application", "vnd.openxmlformats-officedocument.presentationml.presentation"), new LoadOptions());
		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	protected SlidesConvertor(CisConfiguration configuration) {
		super(configuration, MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet().toArray(new MediaType[MEDIA_TYPE_LOAD_FORMAT_MAPPING.size()]));
	}

	@Override
	public void convert(MediaType mediaType, Message message, CisConversionResult result, String charset) throws Exception {
		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}
		try (InputStream inputStream = message.asInputStream(charset)) {
			Presentation presentation = new Presentation(inputStream, MEDIA_TYPE_LOAD_FORMAT_MAPPING.get(mediaType));
			long startTime = new Date().getTime();
			presentation.save(result.getPdfResultFile().getAbsolutePath(), SaveFormat.Pdf);
			long endTime = new Date().getTime();
			LOGGER.info("Conversion(save operation in convert method) takes  :::  " + (endTime - startTime) + " ms");
			presentation.dispose();
			result.setNumberOfPages(getNumberOfPages(result.getPdfResultFile()));
		}
	}

	@Override
	protected boolean isPasswordException(Exception e) {
		return e instanceof InvalidPasswordException;
	}

}
