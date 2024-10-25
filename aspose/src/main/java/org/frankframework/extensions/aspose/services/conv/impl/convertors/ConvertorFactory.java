/*
   Copyright 2019, 2022 WeAreFrank!

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

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.extensions.aspose.services.conv.CisConfiguration;
import org.frankframework.extensions.aspose.services.conv.CisConversionService;

/**
 * Convertor factory instantiates all convertor types and keeps them in a map.
 *
 * @author Gerard van der Hoorn
 *
 */
@Log4j2
public class ConvertorFactory {

	private final Map<MediaType, Convertor> convertorLookupMap = new HashMap<>();

	public ConvertorFactory(CisConversionService cisConversionService, CisConfiguration configuration) {
		addToConvertorLookupMap(new MailConvertor(cisConversionService, configuration));
		addToConvertorLookupMap(new PdfStandaardConvertor(configuration));
		addToConvertorLookupMap(new PdfConvertor(configuration));
		addToConvertorLookupMap(new PdfImageConvertor(configuration));
		addToConvertorLookupMap(new WordConvertor(configuration));
		addToConvertorLookupMap(new CellsConvertor(configuration));
		addToConvertorLookupMap(new SlidesConvertor(configuration));
	}

	private void addToConvertorLookupMap(Convertor convertor) {
		for (MediaType mediaTypeSupported : convertor.getSupportedMediaTypes()) {
			Convertor oldConvertor = convertorLookupMap.put(mediaTypeSupported, convertor);
			if (oldConvertor != null) {
				log.warn("more than one convertor found for [{}]", mediaTypeSupported);
			}
		}
	}

	/**
	 * Return <code>null</code> when no convertor is found.
	 */
	public Convertor getConvertor(MediaType mediaType) {
		return convertorLookupMap.get(mediaType);
	}
}
