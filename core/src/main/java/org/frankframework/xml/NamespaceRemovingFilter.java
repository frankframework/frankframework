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
package org.frankframework.xml;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class NamespaceRemovingFilter extends FullXmlFilter {

	public NamespaceRemovingFilter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		super.startElement("", localName, localName, new NamespaceRemovingAttributesWrapper(atts));
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement("", localName, localName);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		// No-op
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		// No-op
	}
}
