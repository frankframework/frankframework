/*
   Copyright 2021-2023 WeAreFrank!

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

import java.util.ArrayDeque;
import java.util.Deque;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.xml.AttributesWrapper;
import org.frankframework.xml.FullXmlFilter;

public class ElementRoleFilter extends FullXmlFilter {

	private static final String ELEMENT_ROLE_ATTRIBUTE = "elementRole";

	private final Deque<String> elementNames = new ArrayDeque<>();

	public ElementRoleFilter() {
		super();
	}

	public ElementRoleFilter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		String elementName = atts.getValue(ELEMENT_ROLE_ATTRIBUTE);
		if (StringUtils.isEmpty(elementName)) {
			elementName = Character.toLowerCase(localName.charAt(0))+localName.substring(1);
		}
		elementNames.push(elementName);
		super.startElement(uri, elementName, makeQName(uri, elementName), new AttributesWrapper(atts, ELEMENT_ROLE_ATTRIBUTE));
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		String elementName = elementNames.pop();
		super.endElement(uri, elementName, makeQName(uri, elementName));
	}

	protected String makeQName(String uri, String elementName) {
		return (StringUtils.isNotEmpty(uri)? uri+":" :  "" )+elementName;
	}

}
