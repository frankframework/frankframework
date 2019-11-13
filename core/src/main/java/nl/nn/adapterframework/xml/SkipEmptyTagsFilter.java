/*
   Copyright 2019 Integration Partners

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
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SkipEmptyTagsFilter extends FullXmlFilter {

	private boolean pendingElement;
	
	private String uri;
	private String localName;
	private String qName;

	public void handlePendingStartElement() throws SAXException {
		if (pendingElement) {
			super.startElement(uri, localName, qName, new AttributesImpl());
			pendingElement=false;
		}
	}
	
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		handlePendingStartElement();
		super.characters(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (!pendingElement) {
			super.endElement(uri, localName, qName);
		} else {
			pendingElement=false;
		}
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		handlePendingStartElement();
		if (atts.getLength()>0) {
			super.startElement(uri, localName, qName, atts);
		} else {
			pendingElement=true;
			this.uri=uri;
			this.localName=localName;
			this.qName=qName;
		}
	}

}
