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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

public class SkipEmptyTagsFilter extends FullXmlFilter {

	private boolean attributesConsideredContent=false;
	private List<Element> pendingElements = new ArrayList<Element>();
	private Map<String,String> pendingNamespaceMappings = new LinkedHashMap<String,String>();
	
	private boolean nonWhitespaceCharactersSeen=false;
	
	private StringBuffer pendingWhitespace = new StringBuffer();

	public void handlePendingStartElements() throws SAXException {
		for(Element e:pendingElements) {
			for (Entry<String,String> entry: e.namespaceMappings.entrySet()) {
				super.startPrefixMapping(entry.getKey(), entry.getValue());
			}
			super.startElement(e.uri, e.localName, e.qName, e.atts);
			String comments = e.comments.toString();
			if (StringUtils.isNotEmpty(comments)) {
				super.comment(comments.toCharArray(), 0, comments.length());
			}
		}
		pendingElements.clear();
	}
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
		pendingWhitespace.setLength(0);
		if (attributesConsideredContent && atts.getLength()>0) {
			handlePendingStartElements();
			super.startElement(uri, localName, qName, atts);
		} else {
			Element e = new Element();
			e.uri=uri;
			e.localName=localName;
			e.qName=qName;
			e.atts=new AttributesImpl(atts);
			e.namespaceMappings=pendingNamespaceMappings;
			pendingNamespaceMappings=new LinkedHashMap<String,String>();
			pendingElements.add(e);
			e.comments=new StringWriter();
		}
		nonWhitespaceCharactersSeen=false;
	}
	
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (pendingElements.isEmpty()) {
			super.endElement(uri, localName, qName);
		} else {
			pendingElements.remove(pendingElements.size()-1);
			pendingWhitespace.setLength(0);
		}
		nonWhitespaceCharactersSeen=false;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		if (!nonWhitespaceCharactersSeen) {
			boolean nonWhitspaceFound=false;
			for (int i=0;i<length;i++) {
				if (!Character.isWhitespace(ch[i+start])) {
					nonWhitspaceFound=true;
					break;
				}
			}
			if (!nonWhitspaceFound) {
				pendingWhitespace.append(ch,start,length);
				return;
			}
			nonWhitespaceCharactersSeen=true;
		}
		handlePendingStartElements();
		//super.characters(pendingWhitespace.toString().toCharArray(), 0, pendingWhitespace.length());
		pendingWhitespace.setLength(0);
		super.characters(ch, start, length);
	}

	@Override
	public void startCDATA() throws SAXException {
		handlePendingStartElements();
		super.startCDATA();
		nonWhitespaceCharactersSeen=true;
	}

	private class Element {
		public String uri;
		public String localName; 
		public String qName;
		public Attributes atts;
		public Map<String,String> namespaceMappings;
		public StringWriter comments;
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		pendingNamespaceMappings.put(prefix, uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		if (pendingElements.isEmpty()) {
			super.endPrefixMapping(prefix);
		}
	}

	@Override
	public void comment(char[] ch, int start, int length) throws SAXException {
		if (pendingElements.isEmpty()) {
			super.comment(ch, start, length);
		} else {
			pendingElements.get(pendingElements.size()-1).comments.write(ch,start,length);
		}
	}

}
