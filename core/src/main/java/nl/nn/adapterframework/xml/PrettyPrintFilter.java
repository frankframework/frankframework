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

public class PrettyPrintFilter extends FullXmlFilter {

	private String indent="\t";
	private int indentLevel;
	private boolean charactersSeen;
	private boolean elementsSeen;
	private boolean elementContentSeen;
	
	private void write(String string) throws SAXException {
		super.characters(string.toCharArray(), 0, string.length());
	}
	
	private void indent() throws SAXException  {
		write("\n");
		for(int i=0; i<indentLevel; i++) {
			write(indent);
		}
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		if (elementsSeen) {
			indent();
		} else {
			elementsSeen=true;
		}
		super.startElement(uri, localName, qName, atts);
		indentLevel++;
		charactersSeen=false;
		elementContentSeen=false;
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		indentLevel--;
		if (elementContentSeen && !charactersSeen) {
			indent();
		}
		super.endElement(uri, localName, qName);
		charactersSeen=false;
		elementContentSeen=true;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		for (int i=0; !charactersSeen && i<length; i++) {
			if (!Character.isWhitespace(ch[start+i])) {
				charactersSeen=true;
				break;
			}
		}
		if (charactersSeen) {
			super.characters(ch, start, length);
			elementContentSeen=true;
		}
	}

	public void setIndent(String indent) {
		this.indent = indent;
	}

	
}
