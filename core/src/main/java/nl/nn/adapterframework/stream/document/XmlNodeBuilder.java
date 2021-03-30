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
package nl.nn.adapterframework.stream.document;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.xml.SaxDocumentBuilder;
import nl.nn.adapterframework.xml.SaxElementBuilder;

public class XmlNodeBuilder extends NodeBuilder {

	private SaxElementBuilder current;

	public XmlNodeBuilder(String rootElement, ContentHandler handler) throws DocumentException {
		try {
			current = new SaxDocumentBuilder(rootElement, handler);
		} catch (SAXException e) {
			throw new DocumentException(e);
		}
	}
	public XmlNodeBuilder(SaxElementBuilder current, String elementName) throws DocumentException {
		try {
			this.current = current.startElement(elementName);
		} catch (SAXException e) {
			throw new DocumentException(e);
		}
	}
	@Override
	public void close() throws DocumentException {
		try {
			current.close();
		} catch (SAXException e) {
			throw new DocumentException(e);
		}
	}

	@Override
	public ArrayBuilder startArray(String elementName) {
		return new XmlArrayBuilder(current, elementName);
	}

	@Override
	public ObjectBuilder startObject() throws DocumentException {
		return new XmlObjectBuilder(current, null);
	}

	@Override
	public void setValue(String value) throws DocumentException {
		try {
			current.addValue(value).close();
		} catch (SAXException e) {
			throw new DocumentException(e);
		}
	}

	@Override
	public void setValue(long value) throws DocumentException {
		try {
			current.addValue(Long.toString(value)).close();
		} catch (SAXException e) {
			throw new DocumentException(e);
		}
	}

	@Override
	public void setValue(boolean value) throws DocumentException {
		try {
			current.addValue(Boolean.toString(value)).close();
		} catch (SAXException e) {
			throw new DocumentException(e);
		}
	}
	
}
