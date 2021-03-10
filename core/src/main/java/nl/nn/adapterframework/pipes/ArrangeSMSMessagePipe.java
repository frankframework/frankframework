/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.StringWriter;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;

/**
 * Breaks up the text input in blocks of a most 160 characters, to enable them to be send as SMS messages.
 */
public class ArrangeSMSMessagePipe extends FixedForwardPipe {

	private int MAX_BLOCK_LENGTH=160;
	private boolean isSoft = false;

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {

		try {
	
			String result[] = new String[100];
			int p, s, o = 0;
	
			String inputString = message.asString();
	
			if (isSoft) {
				for (p = 0; p < inputString.length() - MAX_BLOCK_LENGTH;) {
					// find last space in msg part
					for (s = p + MAX_BLOCK_LENGTH >= inputString.length() ? inputString.length() - 1 : p + MAX_BLOCK_LENGTH; s >= p
							&& !Character.isWhitespace(inputString.charAt(s)) && inputString.charAt(s) != '-'; s--)
						;
					// now skip spaces
					for (; s >= p && Character.isWhitespace(inputString.charAt(s)); s--)
						;
					// spaces found, soft break possible
					if (s >= p) {
						result[o++] = inputString.substring(p, s + 1);
						for (p = s + 1; p < inputString.length() && Character.isWhitespace(inputString.charAt(p)); p++)
							;
					}
					// no space found, soft-break not possible
					else {
						result[o++] = inputString.substring(p, p + MAX_BLOCK_LENGTH < inputString.length() ? p + MAX_BLOCK_LENGTH : inputString.length());
						p += MAX_BLOCK_LENGTH;
					}
				}
				result[o++] = inputString.substring(p);
			} else {
				for (p = 0; p < inputString.length(); p += MAX_BLOCK_LENGTH) {
					if (p + MAX_BLOCK_LENGTH -1 < inputString.length()) {
						result[o++] = inputString.substring(p, p + MAX_BLOCK_LENGTH);
					} else {
						result[o++] = inputString.substring(p);
					}
				}
			}
	
			try (StringWriter stringWriter = new StringWriter()) {
				XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
		
				XMLStreamWriter xMLStreamWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);
				xMLStreamWriter.writeStartElement("rootTag");
				int counter = 0;
				while (result[counter] != null) {
					xMLStreamWriter.writeStartElement("result");
					xMLStreamWriter.writeCharacters(result[counter]);
					xMLStreamWriter.writeEndElement();
					counter++;
	
				}
				xMLStreamWriter.writeEndElement();
				xMLStreamWriter.flush();
				xMLStreamWriter.close();
				message = new Message(stringWriter.getBuffer().toString());
			}

			return new PipeRunResult(getForward(), message);
		} catch (Exception e) {
			throw new PipeRunException(this, "Cannot create SMS messages", e);
		}
	}

	@IbisDoc({"If true, try to break up the message at spaces, instead of in the middle of words", "false"})
	public void setIsSoft(boolean bool) {
		isSoft = bool;
	}
}