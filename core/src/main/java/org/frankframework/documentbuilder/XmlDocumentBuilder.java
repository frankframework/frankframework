/*
   Copyright 2021, 2022 WeAreFrank!

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

import java.io.StringWriter;
import java.io.Writer;

import org.frankframework.xml.SaxDocumentBuilder;
import org.frankframework.xml.XmlWriter;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class XmlDocumentBuilder extends XmlNodeBuilder implements IDocumentBuilder {

	private Writer writer;
	private SaxDocumentBuilder saxDocumentBuilder;

	public XmlDocumentBuilder(String rootElement, Writer writer) throws SAXException {
		this(new SaxDocumentBuilder(null, new XmlWriter(writer), true), rootElement);
		this.writer = writer;
	}

	public XmlDocumentBuilder(String rootElement, ContentHandler handler, boolean prettyPrint) throws SAXException {
		this(new SaxDocumentBuilder(null, handler, prettyPrint), rootElement);
	}

	private XmlDocumentBuilder(SaxDocumentBuilder saxDocumentBuilder, String rootElement) throws SAXException {
		super(saxDocumentBuilder, rootElement);
		this.saxDocumentBuilder=saxDocumentBuilder;
	}

	@Override
	public String toString() {
		return writer instanceof StringWriter ? writer.toString() : super.toString();
	}

	@Override
	public ObjectBuilder asObjectBuilder() throws SAXException {
		return ObjectBuilder.asObjectBuilder(this);
	}

	@Override
	public ArrayBuilder asArrayBuilder(String elementName) throws SAXException {
		return ArrayBuilder.asArrayBuilder(this, elementName);
	}

	@Override
	public void close() throws SAXException {
		super.close();
		saxDocumentBuilder.close();
	}

}
