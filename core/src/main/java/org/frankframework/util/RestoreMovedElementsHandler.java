/*
   Copyright 2024 WeAreFrank!

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

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.PipeLineSession;
import org.frankframework.xml.FullXmlFilter;

/**
 * Restore moved elements from pipelineSession.
 * Requires a session with the moved elements stored as key-value pairs.
 * The output of the XMLWriter is data with moved elements restored, if they were found.
 */
@Log4j2
public class RestoreMovedElementsHandler extends FullXmlFilter {
	private static final String ME_START = "{sessionKey:";
	private static final String ME_END = "}";
	private boolean inCDATASection = false;

	private final StringBuilder charDataBuilder = new StringBuilder();
	private @Setter PipeLineSession session;

	public RestoreMovedElementsHandler(ContentHandler handler) {
		super(handler);
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		processCharacterData(true);
		super.startElement(uri, localName, qName, attributes);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		processCharacterData(false);
		super.endElement(uri, localName, qName);
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		charDataBuilder.append(ch, start, length);
	}

	private void processCharacterData(boolean startElement) throws SAXException {
		if (inCDATASection) {
			super.startCDATA();
		}
		// Only continue with search & replace, if a session key can be present in the character data.
		int startPos = charDataBuilder.indexOf(ME_START);
		if (startElement || startPos == -1 || session == null || session.isEmpty() || (charDataBuilder.length() <= (ME_START.length() + 1 + ME_END.length()))) {
			writeBuilderAndFinish(charDataBuilder);
			return;
		}

		int copyFrom = 0;
		StringBuilder builder = new StringBuilder();
		while (startPos != -1) {
			builder.append(charDataBuilder, copyFrom, startPos);
			int nextStartPos = charDataBuilder.indexOf(ME_START, startPos + ME_START.length());
			if (nextStartPos == -1) {
				nextStartPos = charDataBuilder.length();
			}
			int endPos = charDataBuilder.indexOf(ME_END, startPos + ME_START.length());
			if (endPos == -1 || endPos > nextStartPos) {
				log.warn("Found a start delimiter without an end delimiter while restoring from compacted result at position [{}] in [{}]", startPos, charDataBuilder);
				builder.append(charDataBuilder, startPos, nextStartPos);
				copyFrom = nextStartPos;
			} else {
				String movedElementSessionKey = charDataBuilder.substring(startPos + ME_START.length(), endPos);
				if (session.containsKey(movedElementSessionKey)) {
					String movedElementValue = session.getString(movedElementSessionKey);
					builder.append(movedElementValue);
					copyFrom = endPos + ME_END.length();
				} else {
					log.warn("Did not find sessionKey [{}] while restoring from compacted result", movedElementSessionKey);
					builder.append(charDataBuilder, startPos, nextStartPos);
					copyFrom = nextStartPos;
				}
			}
			startPos = charDataBuilder.indexOf(ME_START, copyFrom);
		}
		builder.append(charDataBuilder, copyFrom, charDataBuilder.length());
		writeBuilderAndFinish(builder);
	}

	private void writeBuilderAndFinish(StringBuilder builder) throws SAXException {
		super.characters(builder.toString().toCharArray(), 0, builder.length());
		if (inCDATASection) {
			super.endCDATA();
			inCDATASection = false;
		}
		charDataBuilder.setLength(0);
	}

	@Override
	public void startCDATA() throws SAXException {
		inCDATASection = true;
	}

	@Override
	public void endCDATA() throws SAXException {
		// Do nothing, as the CDATA section is already ended in the processCharacterData method.
	}

}
