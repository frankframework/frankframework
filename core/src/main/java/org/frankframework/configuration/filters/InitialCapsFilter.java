/*
   Copyright 2021 WeAreFrank!

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
package org.frankframework.configuration.filters;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.frankframework.util.StringUtil;
import org.frankframework.xml.FullXmlFilter;

public class InitialCapsFilter extends FullXmlFilter {

	public InitialCapsFilter() {
		super();
	}

	public InitialCapsFilter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		super.startElement(uri, initialCap(localName), initialCapQname(qName), atts);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, initialCap(localName), initialCapQname(qName));
	}

	protected String initialCap(String elementName) {
		return StringUtil.ucFirst(elementName);
	}

	protected String initialCapQname(String qname) {
		int colonPos = qname.indexOf(":");
		if (colonPos<0) {
			return initialCap(qname);
		}
		return qname.substring(0,colonPos+1)+initialCap(qname.substring(colonPos+1));
	}
}
