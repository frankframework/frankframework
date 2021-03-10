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
 * Breaks up the text input in blocks of a maximum length. 
 * By default the maximum block length is 160 characters, to enable them to be send as SMS messages.
 */
public class TextSplitterPipe extends FixedForwardPipe {

	private int maxBlockLength=160;
	private boolean softSplit = false;

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {

		try {
	
			String result[] = new String[100];
			int p, s, o = 0;
	
			String inputString = message.asString();
	
			if (softSplit) {
				for (p = 0; p < inputString.length() - maxBlockLength;) {
					// find last space in msg part
					for (s = p + maxBlockLength >= inputString.length() ? inputString.length() - 1 : p + maxBlockLength; s >= p
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
						result[o++] = inputString.substring(p, p + maxBlockLength < inputString.length() ? p + maxBlockLength : inputString.length());
						p += maxBlockLength;
					}
				}
				result[o++] = inputString.substring(p);
			} else {
				for (p = 0; p < inputString.length(); p += maxBlockLength) {
					if (p + maxBlockLength <= inputString.length()) {
						result[o++] = inputString.substring(p, p + maxBlockLength);
					} else {
						result[o++] = inputString.substring(p);
					}
				}
			}
	
			try (StringWriter stringWriter = new StringWriter()) {
				XMLOutputFactory xMLOutputFactory = XMLOutputFactory.newInstance();
		
				XMLStreamWriter xMLStreamWriter = xMLOutputFactory.createXMLStreamWriter(stringWriter);
				xMLStreamWriter.writeStartElement("text");
				int counter = 0;
				while (result[counter] != null) {
					xMLStreamWriter.writeStartElement("block");
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
			throw new PipeRunException(this, "Cannot create text blocks", e);
		}
	}

	@IbisDoc({"1", "Set the maximum number of characters of a block", "160"})
	public void setMaxBlockLength(int maxBlockLength) {
		this.maxBlockLength = maxBlockLength;
	}

	@IbisDoc({"2", "If true, try to break up the message at spaces, instead of in the middle of words", "false"})
	public void setSoftSplit(boolean softSplit) {
		this.softSplit = softSplit;
	}
}