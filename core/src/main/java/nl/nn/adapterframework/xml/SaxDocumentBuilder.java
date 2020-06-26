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

import java.io.Writer;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class SaxDocumentBuilder extends SaxElementBuilder {

	public SaxDocumentBuilder(String elementName) throws SAXException {
		this(elementName,  new XmlWriter());
	}
	
	public SaxDocumentBuilder(String elementName, Writer writer) throws SAXException {
		this(elementName, new XmlWriter(writer));
	}

	public SaxDocumentBuilder(String elementName, ContentHandler handler) throws SAXException {
		super(elementName, handler);
		handler.startDocument();
	}

	@Override
	public void close() throws SAXException {
		try {
			super.close();
		} finally {
			getHandler().endDocument();
		}
	}
	
}
