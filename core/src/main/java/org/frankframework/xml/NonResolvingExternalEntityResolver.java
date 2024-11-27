/*
   Copyright 2013 Nationale-Nederlanden

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
package org.frankframework.xml;

import java.io.IOException;
import java.io.StringReader;

import org.apache.logging.log4j.Logger;
import org.frankframework.util.LogUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * Entity resolver which resolves external entities to an empty string. This
 * will prevent the XML parser from downloading resources as specified by URI's
 * in a DOCTYPE.
 *
 * @author Jaco de Groot
 */
public class NonResolvingExternalEntityResolver implements EntityResolver2 {
	private final Logger log = LogUtil.getLogger(this);

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		return resolveEntity(null, publicId, null, systemId);
	}

	@Override
	public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
		return null;
	}

	@Override
	public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
		log.warn("Resolving entity with name [{}], public id [{}], base uri [{}] and system id [{}] to an empty string", name, publicId, baseURI, systemId);
		return new InputSource(new StringReader(""));
	}

}
