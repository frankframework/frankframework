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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

/**
 * XmlFilter that skips startDocument and endDocument, 
 * to be able to parse some additional body elements into and existing XML stream.
 * 
 * @author Gerrit van Brakel
 *
 */
public class BodyOnlyFilter extends FullXmlFilter {

	private @Getter @Setter boolean skipRoot=true;

	private int level;

	public BodyOnlyFilter() {
		super();
	}

	public BodyOnlyFilter(ContentHandler handler) {
		super(handler);
	}

	public BodyOnlyFilter(ContentHandler handler, boolean skipRoot) {
		this(handler);
		this.skipRoot = skipRoot;
	}

	@Override
	public void endDocument() throws SAXException {
		// skip this method
	}

	@Override
	public void startDocument() throws SAXException {
		// skip this method
	}

	@Override
	public void startElement(String uri, String localname, String qname, Attributes attributes) throws SAXException {
		if (!isSkipRoot() || level++>0) {
			super.startElement(uri, localname, qname, attributes);
		}
	}

	@Override
	public void endElement(String uri, String localname, String qname) throws SAXException {
		if (!isSkipRoot() || --level>0) {
			super.endElement(uri, localname, qname);
		}
	}

}
