/*
   Copyright 2020 Integration Partners B.V.

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

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class PrefixMappingObservingFilter extends PrettyPrintFilter {

	public PrefixMappingObservingFilter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		AttributesImpl attributes = new AttributesImpl();
		attributes.addAttribute("", "uri", "uri", "string", uri);
		startElement("", "prefix-"+prefix, "prefix-"+prefix, attributes);
		super.startPrefixMapping(prefix, uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		super.endPrefixMapping(prefix);
		endElement("", "prefix-"+prefix, "prefix-"+prefix);
	}

}
