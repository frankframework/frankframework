/*
   Copyright 2019 Integration Partners

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

import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.apache.tika.mime.MediaType;

import com.aspose.slides.InvalidPasswordException;
import com.aspose.slides.LoadOptions;
import com.aspose.slides.Presentation;
import com.aspose.slides.SaveFormat;

import nl.nn.adapterframework.extensions.aspose.ConversionOption;
import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionResult;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Converts the files which are required and supported by the aspose slides
 * library.
 * 
 * @author M64D844
 *
 */
public class SlidesConvertor extends AbstractConvertor {

	private static final Logger LOGGER = LogUtil.getLogger(SlidesConvertor.class);
	// contains mapping from MediaType to the LoadOption for the aspose word
	// conversion.
	private static final Map<MediaType, LoadOptions> MEDIA_TYPE_LOAD_FORMAT_MAPPING;

	static {
		Map<MediaType, LoadOptions> map = new HashMap<>();

		// The string value is defined in com.aspose.slides.LoadOptions.
		map.put(new MediaType("application", "vnd.ms-powerpoint"), new LoadOptions());
		map.put(new MediaType("application", "vnd.openxmlformats-officedocument.presentationml.presentation"),
				new LoadOptions());

		MEDIA_TYPE_LOAD_FORMAT_MAPPING = Collections.unmodifiableMap(map);
	}

	SlidesConvertor(String pdfOutputLocation) {
		// Give the supported media types.
		super(pdfOutputLocation,
				MEDIA_TYPE_LOAD_FORMAT_MAPPING.keySet().toArray(new MediaType[MEDIA_TYPE_LOAD_FORMAT_MAPPING.size()]));
	}

	@Override
	public void convert(MediaType mediaType, File file, CisConversionResult result, ConversionOption conversionOption)
			throws Exception {

		if (!MEDIA_TYPE_LOAD_FORMAT_MAPPING.containsKey(mediaType)) {
			// mediaType should always be supported otherwise there a program error because
			// the supported media types should be part of the map
			throw new IllegalArgumentException("Unsupported mediaType " + mediaType + " should never happen here!");
		}
		try (FileInputStream inputStream = new FileInputStream(file)) {
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
