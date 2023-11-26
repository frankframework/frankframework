/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.xml.FullXmlFilter;
import nl.nn.adapterframework.xml.WritableAttributes;

public class ClassNameRewriter extends FullXmlFilter {

	public ClassNameRewriter(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		WritableAttributes writableAttributes = new WritableAttributes(attributes);
		String className = writableAttributes.getValue("className");
		if (className != null) {
			if (className.startsWith("nl.nn.adapterframework.")) {
				writableAttributes.setValue("className", className.replace("nl.nn.adapterframework.", "org.frankframework."));
			}
		}
		super.startElement(uri, localName, qName, writableAttributes);
	}
}
