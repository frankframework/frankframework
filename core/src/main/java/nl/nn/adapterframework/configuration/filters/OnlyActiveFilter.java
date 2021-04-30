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
package nl.nn.adapterframework.configuration.filters;

import java.util.Properties;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.xml.AttributesWrapper;
import nl.nn.adapterframework.xml.FullXmlFilter;

public class OnlyActiveFilter extends FullXmlFilter {

	private final String ACTIVE_ATTRIBUTE = "active";
	private Properties properties;
	private int suppressLevel = 0;

	/**
	 * Filter out elements which have an ACTIVE attribute set to false
	 */
	public OnlyActiveFilter(ContentHandler resolver) {
		this(resolver, null);
	}

	/**
	 * Filter out elements which have an ACTIVE attribute set to false.
	 * Attempt to resolve the active attribute using provided Properties
	 */
	public OnlyActiveFilter(ContentHandler resolver, Properties properties) {
		super(resolver);
		this.properties = properties;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (suppressLevel>0) {
			suppressLevel++;
			return;
		}

		String active = atts.getValue(ACTIVE_ATTRIBUTE);
		if(active != null && properties != null) {
			active = StringResolver.substVars(active, properties);
		}

		//If an active property is present but EMPTY, assume false.
		if (active != null && !(active.equalsIgnoreCase("true") || active.equalsIgnoreCase("!false"))) {
			suppressLevel = 1;
			return;
		}

		super.startElement(uri, localName, qName, new AttributesWrapper(atts, ACTIVE_ATTRIBUTE));
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (suppressLevel>0) {
			return;
		}
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (suppressLevel>0) {
			suppressLevel--;
			return;
		}

		super.endElement(uri, localName, qName);
	}
}
