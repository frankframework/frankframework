/*
   Copyright 2016 Nationale-Nederlanden, 2023 WeAreFrank!

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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import lombok.Getter;
import lombok.Setter;

/**
 * SAX2 event handler to compact XML messages.
 *
 * @author Peter Leeuwenburgh
 */
@Setter
public class CompactSaxHandler extends DefaultHandler {
	private static final String VALUE_MOVE_START = "{sessionKey:";
	private static final String VALUE_MOVE_END = "}";

	@Getter private String chompCharSize = null;
	@Getter private int chompLength = -1;
	@Getter private String elementToMove = null;
	@Getter private String elementToMoveSessionKey = null;
	@Getter private String elementToMoveChain = null;
	@Getter private boolean removeCompactMsgNamespaces = true;

	private final StringBuilder messageBuilder = new StringBuilder();
	private final StringBuilder charDataBuilder = new StringBuilder();
	private final StringBuilder namespaceBuilder = new StringBuilder();
	private final List<String> elements = new ArrayList<>();
	private Map<String, Object> context = null;

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		printCharData();
		elements.add(localName);

		StringBuilder attributeBuffer = new StringBuilder();
		for (int i = 0; i < attributes.getLength(); i++) {
			attributeBuffer.append(" ");
			if (isRemoveCompactMsgNamespaces()) {
				attributeBuffer.append(attributes.getLocalName(i));
			} else {
				attributeBuffer.append(attributes.getQName(i));
			}
			attributeBuffer.append("=\"");
			attributeBuffer.append(attributes.getValue(i));
			attributeBuffer.append("\"");
		}

		if (isRemoveCompactMsgNamespaces()) {
			messageBuilder.append("<").append(localName).append(attributeBuffer).append(">");
		} else {
			messageBuilder.append("<").append(qName).append(namespaceBuilder).append(attributeBuffer).append(">");
		}
		namespaceBuilder.setLength(0);
	}

	@Override
	public void startPrefixMapping(String prefix, String uri) {
		String thisPrefix = "";
		if (StringUtils.isNotEmpty(prefix)) {
			thisPrefix = ":" + prefix;
		}
		if (StringUtils.isNotEmpty(uri)) {
			namespaceBuilder.append(" xmlns").append(thisPrefix).append("=\"").append(uri).append("\"");
		}
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		int lastIndex = elements.size() - 1;
		String lastElement = elements.get(lastIndex);
		if (!lastElement.equals(localName)) {
			throw new SAXException("expected end element [" + lastElement + "] but got end element [" + localName + "]");
		}

		printCharData();
		if (isRemoveCompactMsgNamespaces()) {
			messageBuilder.append("</").append(localName).append(">");
		} else {
			messageBuilder.append("</").append(qName).append(">");
		}
		elements.remove(lastIndex);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		charDataBuilder.append(ch, start, length);
	}

	private void printCharData() {
		if (charDataBuilder.length() == 0) {
			return;
		}

		String before = "";
		String after = "";

		if (chompLength >= 0 && charDataBuilder.length() > chompLength) {
			before = "*** character data size [" + charDataBuilder.length() + "] exceeds [" + getChompCharSize() + "] and is chomped ***";
			after = "...(" + (charDataBuilder.length() - chompLength) + " characters more)";
			charDataBuilder.setLength(chompLength);
		}

		int lastIndex = elements.size() - 1;
		String lastElement = elements.get(lastIndex);

		if (context != null
				&& (getElementToMove() != null && lastElement.equals(getElementToMove()) ||
				(getElementToMoveChain() != null && elementsToString().equals(getElementToMoveChain())))
				&& !(charDataBuilder.toString().startsWith(VALUE_MOVE_START) && charDataBuilder.toString().endsWith(VALUE_MOVE_END))) {
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
			context.put(elementToMoveSK, before + charDataBuilder + after);
			messageBuilder.append(VALUE_MOVE_START).append(elementToMoveSK).append(VALUE_MOVE_END);
		} else {
			messageBuilder.append(before).append(XmlEncodingUtils.encodeChars(charDataBuilder.toString())).append(after);
		}

		charDataBuilder.setLength(0);
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

	public String getXmlString() {
		return messageBuilder.toString();
	}

	public void setChompCharSize(String input) {
		chompCharSize = input;
		chompLength = (int) Misc.toFileSize(chompCharSize, -1);
	}

}
