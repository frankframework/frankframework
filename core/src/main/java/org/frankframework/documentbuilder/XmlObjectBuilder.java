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
package org.frankframework.documentbuilder;

import org.frankframework.util.XmlUtils;
import org.frankframework.xml.SaxElementBuilder;
import org.xml.sax.SAXException;

public class XmlObjectBuilder extends ObjectBuilder {

	private final SaxElementBuilder current;

	public XmlObjectBuilder(SaxElementBuilder current, String elementName) throws SAXException {
		this.current = current.startElement(XmlUtils.cleanseElementName(elementName));
	}

	@Override
	public void close() throws SAXException {
		try {
			current.close();
		} finally {
			super.close();
		}
	}

	@Override
	public INodeBuilder addField(String fieldName) throws SAXException {
		return new XmlNodeBuilder(current, fieldName);
	}

	@Override
	public void addAttribute(String name, String value) throws SAXException {
		current.addAttribute(XmlUtils.cleanseElementName(name), value);
	}

	@Override
	public void addAttribute(String name, Number value) throws SAXException {
		addAttribute(name, value.toString());
	}
	@Override
	public void addAttribute(String name, boolean value) throws SAXException {
		addAttribute(name, Boolean.toString(value));
	}
	@Override
	public void addNumberAttribute(String name, String value) throws SAXException {
		addAttribute(name, value);
	}

	@Override
	public ArrayBuilder addRepeatedField(String fieldName) throws SAXException {
		return new XmlArrayBuilder(current, fieldName);
	}

}
