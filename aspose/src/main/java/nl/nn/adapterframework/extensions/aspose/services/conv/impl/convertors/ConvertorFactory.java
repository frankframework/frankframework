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
package nl.nn.adapterframework.extensions.aspose.services.conv.impl.convertors;

import java.util.HashMap;
import java.util.Map;

import nl.nn.adapterframework.extensions.aspose.services.conv.CisConfiguration;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;

import nl.nn.adapterframework.extensions.aspose.services.conv.CisConversionService;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Convertor factory instantiates all convertor types and keeps them in a map.
 *
 * @author Gerard van der Hoorn
 *
 */
public class ConvertorFactory {

	private static final Logger LOGGER = LogUtil.getLogger(ConvertorFactory.class);

	private Map<MediaType, Convertor> convertorLookupMap = new HashMap<>();

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
				LOGGER.warn("More than one convertor found for " + mediaTypeSupported);
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
