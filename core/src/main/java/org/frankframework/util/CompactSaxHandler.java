/*
   Copyright 2016 Nationale-Nederlanden, 2023-2025 WeAreFrank!

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
package org.frankframework.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.stream.Message;
import org.frankframework.xml.FullXmlFilter;
import org.frankframework.xml.NamespaceRemovingAttributesWrapper;

/**
 * SAX2 event handler to compact XML messages.
 *
 * @author Peter Leeuwenburgh
 */
public class CompactSaxHandler extends FullXmlFilter {
	private static final String VALUE_MOVE_START = "{sessionKey:";
	private static final String VALUE_MOVE_END = "}";

	@Getter @Setter private int chompLength = -1;
	@Getter @Setter private String elementToMove = null;
	@Getter @Setter private String elementToMoveSessionKey = null;
	@Getter @Setter private String elementToMoveChain = null;
	@Getter @Setter private boolean removeCompactMsgNamespaces = true;

	private final StringBuilder charDataBuilder = new StringBuilder();
	private final List<String> elements = new ArrayList<>();
	@Setter private Map<String, Object> context = null;
	private boolean moveElementFound = false;
	private boolean inCDATASection = false;

	public CompactSaxHandler(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		printCharData(true);
		elements.add(localName);

		moveElementFound = context != null &&
				(getElementToMove() != null && localName.equals(getElementToMove())) ||
				(getElementToMoveChain() != null && elementsToString().equals(getElementToMoveChain()));

		if (isRemoveCompactMsgNamespaces()) {
			super.startElement(uri, localName, localName, new NamespaceRemovingAttributesWrapper(attributes));
		} else {
			super.startElement(uri, localName, qName, attributes);
		}
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		if (!isRemoveCompactMsgNamespaces()) {
			super.startPrefixMapping(prefix, uri);
		}
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		if (!isRemoveCompactMsgNamespaces()) {
			super.endPrefixMapping(prefix);
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		int lastIndex = elements.size() - 1;
		String lastElement = elements.get(lastIndex);
		if (!lastElement.equals(localName)) {
			throw new SAXException("expected end element [" + lastElement + "] but got end element [" + localName + "]");
		}

		printCharData(false);
		if (isRemoveCompactMsgNamespaces()) {
			super.endElement(uri, localName, localName);
		} else {
			super.endElement(uri, localName, qName);
		}
		elements.remove(lastIndex);
	}

	@Override
	public void startCDATA() throws SAXException {
		inCDATASection = true;
	}

	@Override
	public void endCDATA() throws SAXException {
		// No-op
	}

	@Override
	public void characters(char[] ch, int start, int length) {
		charDataBuilder.append(ch, start, length);
	}

	private void printCharData(boolean startElement) throws SAXException {
		if (charDataBuilder.isEmpty()) {
			return;
		}
		if (inCDATASection) {
			super.startCDATA();
		}

		// Detection Moving elements; only when not already moved session key is found
		int length = charDataBuilder.length();
		moveElementFound = moveElementFound
				&& context != null
				&& !startElement
				&& !(length > VALUE_MOVE_START.length()
				&& charDataBuilder.substring(length - 1, length).equals(VALUE_MOVE_END)
				&& charDataBuilder.substring(0, VALUE_MOVE_START.length()).equals(VALUE_MOVE_START));

		if (moveElementFound) {
			Message message = new Message(charDataBuilder.toString());
			int lastIndex = elements.size() - 1;
			String lastElement = elements.get(lastIndex);
			String elementToMoveSK = determineElementToMoveSessionKey(lastElement);
			context.put(elementToMoveSK, message);

			super.characters(VALUE_MOVE_START.toCharArray(), 0, VALUE_MOVE_START.length());
			super.characters(elementToMoveSK.toCharArray(), 0, elementToMoveSK.length());
			super.characters(VALUE_MOVE_END.toCharArray(), 0, 1);
			moveElementFound = false;
		} else {
			String after = null;
			if (chompLength >= 0 && charDataBuilder.length() > chompLength) {
				String before = "*** character data size [" + charDataBuilder.length() + "] exceeds [" + chompLength + "] and is chomped ***";
				after = "...(" + (charDataBuilder.length() - chompLength) + " characters more)";
				charDataBuilder.setLength(chompLength);
				super.characters(before.toCharArray(), 0, before.length());
			}
			char[] charData = charDataBuilder.toString().toCharArray();
			super.characters(charData, 0, charData.length);
			if (after != null) {
				super.characters(after.toCharArray(), 0, after.length());
			}
		}

		if (inCDATASection) {
			super.endCDATA();
		}
		inCDATASection = false;
		charDataBuilder.setLength(0);
	}

	private String determineElementToMoveSessionKey(final String lastElement) {
		String elementToMoveSK;
		if (getElementToMoveSessionKey() == null) {
			elementToMoveSK = "ref_" + lastElement;
		} else {
			elementToMoveSK = getElementToMoveSessionKey();
		}
		if (context.containsKey(elementToMoveSK)) {
			String baseName = elementToMoveSK;
			int counter = 1;
			while (context.containsKey(elementToMoveSK)) {
				counter++;
				elementToMoveSK = baseName + counter;
			}
		}
		return elementToMoveSK;
	}

	private String elementsToString() {
		String chain = null;
		for (String element : elements) {
			if (chain == null) {
				chain = element;
			} else {
				chain = chain.concat(";").concat(element);
			}
		}
		return chain;
	}

	public void setChompCharSize(String input) {
		chompLength = (int) Misc.toFileSize(input, -1);
	}

}
