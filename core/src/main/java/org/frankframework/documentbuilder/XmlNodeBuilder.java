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

import org.xml.sax.SAXException;

import org.frankframework.util.XmlUtils;
import org.frankframework.xml.SaxElementBuilder;

public class XmlNodeBuilder implements INodeBuilder {

	private final SaxElementBuilder current;

	public XmlNodeBuilder(SaxElementBuilder current, String elementName) throws SAXException {
		this.current = current.startElement(XmlUtils.cleanseElementName(elementName));
	}

	@Override
	public void close() throws SAXException {
		current.close();
	}

	@Override
	public XmlArrayBuilder startArray(String elementName) {
		return new XmlArrayBuilder(current, elementName);
	}

	@Override
	public XmlObjectBuilder startObject() throws SAXException {
		return new XmlObjectBuilder(current, null);
	}

	@Override
	public void setValue(String value) throws SAXException {
		if (value!=null) {
			current.addValue(value).close();
		} else {
			current.addAttribute("nil", "true").close();
		}
	}

	@Override
	public void setValue(Number value) throws SAXException {
		current.addValue(value.toString()).close();
	}

	@Override
	public void setValue(boolean value) throws SAXException {
		current.addValue(Boolean.toString(value)).close();
	}

	@Override
	public void setNumberValue(String value) throws SAXException {
		setValue(value);
	}

}
