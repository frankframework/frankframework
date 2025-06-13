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
package org.frankframework.configuration.filters;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import org.frankframework.xml.FullXmlFilter;

/**
 * This class omits XML elements from Frank configs that are meaningless containers.
 * The children of the omitted elements are kept. This class does its job by filtering SAX events.
 *
 */
public class SkipContainersFilter extends FullXmlFilter {

	// Here are the omitted XML elements.
	//
	// The Frank!Doc trusts that this class is used to omit the <Module> element.
	// If you ever want to remove Module from this list, please update the Frank!Doc.
	// To search for the relevant Frank!Doc code, you can start at
	// org.frankframework.frankdoc.Constants.MODULE_ELEMENT_NAME.
	private static final String[] SKIPABLE_CONTAINERS = { "Exits", "Forwards", "Module", "Root", "GlobalForwards", "Global-forwards", "Scheduler" };
	private final Set<String> skipableContainers = new LinkedHashSet<>(Arrays.asList(SKIPABLE_CONTAINERS));

	public SkipContainersFilter() {
		super();
	}

	public SkipContainersFilter(ContentHandler handler) {
		super(handler);
	}

	protected boolean isContainer(String localName) {
		return skipableContainers.contains(localName);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (!isContainer(localName)) {
			super.startElement(uri, localName, qName, atts);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (!isContainer(localName)) {
			super.endElement(uri, localName, qName);
		}
	}

}
