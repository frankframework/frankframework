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
package nl.nn.adapterframework.xml;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Filter that removes all Elements and attributes that are in a namespace, retaining only non-namespaced content.
 * 
 * @author Gerrit van Brakel
 *
 */
public class NamespacedContentsRemovingFilter extends FullXmlFilter {

	private int removingDepth=0;

	public NamespacedContentsRemovingFilter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (removingDepth>0 || StringUtils.isNotEmpty(uri)) {
			removingDepth++;
		} else {
			super.startElement("", localName, localName, new NamespacedContentsRemovingAttributesWrapper(atts));
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (removingDepth>0) {
			removingDepth--;
		} else {
			super.endElement("", localName, localName);
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		//log.debug("startPrefixMapping("+prefix+","+uri+")");
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		//log.debug("endPrefixMapping("+prefix+")");
	}

	@Override
	public void startCDATA() throws SAXException {
		if (removingDepth==0) {
			super.startCDATA();
		}
	}
	@Override
	public void endCDATA() throws SAXException {
		if (removingDepth==0) {
			super.endCDATA();
		}
	}

	@Override
	public void characters(char[] ch, int offset, int length) throws SAXException {
		if (removingDepth==0) {
			super.characters(ch, offset, length);
		}
	}

}