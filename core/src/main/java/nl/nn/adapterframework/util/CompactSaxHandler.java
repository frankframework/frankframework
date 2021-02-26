/*
   Copyright 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * SAX2 event handler to compact XML messages.
 * 
 * @author  Peter Leeuwenburgh
 */
public class CompactSaxHandler extends DefaultHandler {
	private final static String VALUE_MOVE_START = "{sessionKey:";
	private final static String VALUE_MOVE_END = "}";

	private String chompCharSize = null;
	private int chompLength = -1;
	private String elementToMove = null;
	private String elementToMoveSessionKey = null;
	private String elementToMoveChain = null;
	private boolean removeCompactMsgNamespaces = true;

	private StringBuffer messageBuffer = new StringBuffer();
	private StringBuffer charBuffer = new StringBuffer();
	private StringBuffer namespaceBuffer = new StringBuffer();
	private List<String> elements = new ArrayList<>();
	private Map<String,Object> context = null;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

		printCharBuffer();

		StringBuffer attributeBuffer = new StringBuffer();
		for (int i = 0; i < attributes.getLength(); i++) {
			attributeBuffer.append(" ");
			attributeBuffer.append(attributes.getQName(i));
			attributeBuffer.append("=\"");
			attributeBuffer.append(attributes.getValue(i));
			attributeBuffer.append("\"");
		}

		if (isRemoveCompactMsgNamespaces()) {
			messageBuffer.append("<" + localName + attributeBuffer.toString() + ">");
		} else {
			messageBuffer.append("<" + qName + namespaceBuffer + attributeBuffer.toString() + ">");
		}
		elements.add(localName);
		namespaceBuffer.setLength(0);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) {
		String thisPrefix = "";
		if (prefix != "") {
			thisPrefix = ":" + prefix;
		}
		if (uri != "") {
			namespaceBuffer.append(" xmlns" + thisPrefix + "=\"" + uri + "\"");
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		int lastIndex = elements.size() - 1;
		String lastElement = (String) elements.get(lastIndex);
		if (!lastElement.equals(localName)) {
			throw new SAXException("expected end element [" + lastElement + "] but got end element [" + localName + "]");
		}

		printCharBuffer();
		if (isRemoveCompactMsgNamespaces()) {
			messageBuffer.append("</" + localName + ">");
		} else {
			messageBuffer.append("</" + qName + ">");
		}
		elements.remove(lastIndex);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		charBuffer.append(ch, start, length);
	}

	private void printCharBuffer() {
		if (charBuffer.length() > 0) {
			String before = "";
			String after = "";

			if (chompLength >= 0 && charBuffer.length() > chompLength) {
				before = "*** character data size [" + charBuffer.length() + "] exceeds [" + getChompCharSize() + "] and is chomped ***";
				after = "...(" + (charBuffer.length() - chompLength) + " characters more)";
				charBuffer.setLength(chompLength);
			}

			int lastIndex = elements.size() - 1;
			String lastElement = (String) elements.get(lastIndex);

			if (context != null
					&& ((getElementToMove() != null && lastElement.equals(getElementToMove()) || (getElementToMoveChain() != null && elementsToString().equals(getElementToMoveChain()))))
					&& !(charBuffer.toString().startsWith(VALUE_MOVE_START) && charBuffer.toString().endsWith(VALUE_MOVE_END))) {
				String elementToMoveSK;
				if (getElementToMoveSessionKey() == null) {
					elementToMoveSK = "ref_" + lastElement;
				} else {
					elementToMoveSK = getElementToMoveSessionKey();
				}
				if (context.containsKey(elementToMoveSK)) {
					String etmsk = elementToMoveSK;
					int counter = 1;
					while (context.containsKey(elementToMoveSK)) {
						counter++;
						elementToMoveSK = etmsk + counter;
					}
				}
				context.put(elementToMoveSK, before + charBuffer.toString() + after);
				messageBuffer.append(VALUE_MOVE_START + elementToMoveSK + VALUE_MOVE_END);
			} else {
				messageBuffer.append(before + XmlUtils.encodeChars(charBuffer.toString()) + after);
			}

			charBuffer.setLength(0);
		}
	}

	private String elementsToString() {
		String chain = null;
		for (Iterator<String> it = elements.iterator(); it.hasNext();) {
			String element = (String) it.next();
			if (chain == null) {
				chain = element;
			} else {
				chain = chain + ";" + element;
			}
		}
		return chain;
	}

	public void setContext(Map<String,Object> map) {
		context = map;
	}

	public String getXmlString() {
		return messageBuffer.toString();
	}

	public void setChompCharSize(String string) {
		chompCharSize = string;
		chompLength = (int) Misc.toFileSize(chompCharSize, -1);
	}

	public String getChompCharSize() {
		return chompCharSize;
	}

	public void setElementToMove(String string) {
		elementToMove = string;
	}

	public String getElementToMove() {
		return elementToMove;
	}

	public void setElementToMoveSessionKey(String string) {
		elementToMoveSessionKey = string;
	}

	public String getElementToMoveSessionKey() {
		return elementToMoveSessionKey;
	}

	public void setElementToMoveChain(String string) {
		elementToMoveChain = string;
	}

	public String getElementToMoveChain() {
		return elementToMoveChain;
	}

	public void setRemoveCompactMsgNamespaces(boolean b) {
		removeCompactMsgNamespaces = b;
	}

	public boolean isRemoveCompactMsgNamespaces() {
		return removeCompactMsgNamespaces;
	}
}
