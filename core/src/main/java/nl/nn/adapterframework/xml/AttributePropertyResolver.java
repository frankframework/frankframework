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

import java.util.List;
import java.util.Properties;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringResolver;


public class AttributePropertyResolver extends FullXmlFilter {
	private Properties properties;
	private List<String> propsToHide;
	private static final boolean resolveWithPropertyName = AppConstants.getInstance().getBoolean("properties.resolve.withName", false);

	public AttributePropertyResolver(Properties properties) {
		this(new XmlWriter(), properties, null);
	}

	public AttributePropertyResolver(ContentHandler handler, Properties properties, List<String> propsToHide) {
		super(handler);
		if(properties == null) {
			throw new IllegalArgumentException("no properties defined");
		}
		this.properties = properties;
		this.propsToHide = propsToHide;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, new AttributesWrapper(attributes, v->StringResolver.substVars(v, properties, null, propsToHide, resolveWithPropertyName)));
	}
}
