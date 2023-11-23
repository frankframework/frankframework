/*
   Copyright 2017 Nationale-Nederlanden

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
package nl.nn.adapterframework.align;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.align.content.MapContentContainer;

/**
 * XML Schema guided XML to JSON converter;
 * 
 * @author Gerrit van Brakel
 */
public class Xml2Map<V> extends XmlTo<MapContentContainer<V>> {

	public Xml2Map(XmlAligner aligner, Map<String,List<V>> data) {
		super(aligner, new MapContentContainer<V>(data));
	}

	public static Map<String,String> translate(String xml, URL schemaURL) throws SAXException, IOException {
		Map<String,List<String>> map = new LinkedHashMap<>();
		MapContentContainer<String> documentContainer = new MapContentContainer<>(map);
		translate(xml, schemaURL, documentContainer);
		return documentContainer.flattenedHorizontal();
	}

}
