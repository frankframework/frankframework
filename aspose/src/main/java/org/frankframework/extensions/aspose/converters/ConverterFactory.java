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

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.CisConfiguration;

/**
 * Converter factory instantiates all convertor types and keeps them in a map.
 *
 * @author Gerard van der Hoorn
 *
 */
@Log4j2
public class ConverterFactory {

	private final Map<MediaType, Converter> convertorLookupMap = new HashMap<>();

	public ConverterFactory(CisConfiguration configuration) {
		addToConvertorLookupMap(new MailConverter(configuration));
		addToConvertorLookupMap(new NoOpConverter(configuration));
		addToConvertorLookupMap(new XpsConverter(configuration));
		addToConvertorLookupMap(new ImageConverter(configuration));
		addToConvertorLookupMap(new WordConverter(configuration));
		addToConvertorLookupMap(new CellsConverter(configuration));
		addToConvertorLookupMap(new SlidesConverter(configuration));
	}

	private void addToConvertorLookupMap(Converter convertor) {
		for (MediaType mediaTypeSupported : convertor.getSupportedMediaTypes()) {
			Converter oldConvertor = convertorLookupMap.put(mediaTypeSupported, convertor);
			if (oldConvertor != null) {
				log.warn("more than one convertor found for [{}]", mediaTypeSupported);
			}
		}
	}

	/**
	 * Return <code>null</code> when no converter is found.
	 */
	public Converter getConvertor(MediaType mediaType) {
		return convertorLookupMap.get(mediaType);
	}
}
