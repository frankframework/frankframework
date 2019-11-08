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
import org.xml.sax.XMLReader;

/**
 * Filter that copies only a single element type, and its contents.
 * 
 * @author Gerrit van Brakel
 */
public class ContainerElementFilter extends FullXmlFilter {

	private String containerElement;
	
	private int level;
	
	public ContainerElementFilter(String containerElement) {
		super();
		this.containerElement=containerElement;
	}

	public ContainerElementFilter(String containerElement, XMLReader parent) {
		super(parent);
		this.containerElement=containerElement;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (level<=0 && localName.equals(containerElement)) {
			super.startElement(uri, localName, qName, atts);
			level=1;
		} else {
			if (level>0) {
				super.startElement(uri, localName, qName, atts);
				level++;
			}
		}
	}


	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (level-->0) {
			super.endElement(uri, localName, qName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.characters(ch, start, length);
		}
	}


	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.comment(ch, start, length);
		}
	}

	@Override
	public void startCDATA() throws SAXException {
		if (level>0) {
			super.startCDATA();
		}
	}

	@Override
	public void endCDATA() throws SAXException {
		if (level>0) {
			super.endCDATA();
		}
	}

	@Override
	public void startEntity(String name) throws SAXException {
		if (level>0) {
			super.startEntity(name);
		}
	}

	@Override
	public void endEntity(String name) throws SAXException {
		if (level>0) {
			super.endEntity(name);
		}
	}

	@Override
	public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
		if (level>0) {
			super.ignorableWhitespace(ch, start, length);
		}
	}


}
