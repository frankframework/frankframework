/*
   Copyright 2022 WeAreFrank!

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

import java.util.Properties;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

public class AttributePropertyResolver extends FullXmlFilter {
	private Properties properties;

	public AttributePropertyResolver(Properties properties) {
		this(new XmlWriter(), properties);
	}

	public AttributePropertyResolver(ContentHandler writer, Properties properties) {
		super(writer);
		if(properties == null) {
			throw new IllegalArgumentException("no properties defined");
		}
		this.properties = properties;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		//TODO loop through all attributes and StringResolver.substVars(attributes.getValue(i), properties)
		super.startElement(uri, localName, qName, attributes);
	}
}
