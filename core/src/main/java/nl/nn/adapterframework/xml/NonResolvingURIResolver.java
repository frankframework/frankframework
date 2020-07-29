/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.xml;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

/**
 * Entity resolver which resolves external entities to an empty string. This
 * will prevent the XML parser from downloading resources as specified by URI's
 * in a DOCTYPE.
 */
public class NonResolvingURIResolver implements URIResolver {
	private Logger log = LogUtil.getLogger(this);

	@Override
	public Source resolve(String href, String base) throws TransformerException {
		log.warn("resolving entity with href [" + href + "] base [" + base + "] to NULL");

		return null;
	}
}
