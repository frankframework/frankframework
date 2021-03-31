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

import org.xml.sax.SAXException;

import nl.nn.adapterframework.xml.SaxElementBuilder;

public class XmlArrayBuilder extends ArrayBuilder {

	private SaxElementBuilder current;
	private String elementName;


	public XmlArrayBuilder(SaxElementBuilder current, String elementName) {
		this.elementName = elementName;
		this.current = current;
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
	public INodeBuilder addElement() throws SAXException {
		return new XmlNodeBuilder(current, elementName);
	}
}
