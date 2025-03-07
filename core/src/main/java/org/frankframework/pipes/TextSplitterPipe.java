/*
   Copyright 2013 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.pipes;


import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageBuilder;
import org.frankframework.xml.SaxDocumentBuilder;

/**
 * Breaks up the text input in blocks of a maximum length.
 * By default, the maximum block length is 160 characters, to enable them to be sent as SMS messages.
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class TextSplitterPipe extends FixedForwardPipe {

	private int maxBlockLength=160;
	private boolean softSplit = false;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (Message.isNull(message) || message.isEmpty()) {
			return new PipeRunResult(getSuccessForward(), new Message("<text/>"));
		}

		try {
			String[] result = new String[100];
			int p, s, o = 0;
			String inputString = message.asString();

			if (softSplit) {
				for (p = 0; p < inputString.length() - maxBlockLength;) {
					// find last space in msg part
					for (s = p + maxBlockLength >= inputString.length() ? inputString.length() - 1 : p + maxBlockLength; s >= p
							&& !Character.isWhitespace(inputString.charAt(s)) && inputString.charAt(s) != '-'; s--);

					// now skip spaces
					for (; s >= p && Character.isWhitespace(inputString.charAt(s)); s--);

					// spaces found, soft break possible
					if (s >= p) {
						result[o++] = inputString.substring(p, s + 1);
						for (p = s + 1; p < inputString.length() && Character.isWhitespace(inputString.charAt(p)); p++);
					}
					// no space found, soft-break not possible
					else {
						result[o++] = inputString.substring(p, Math.min(p + maxBlockLength, inputString.length()));
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

			MessageBuilder messageBuilder = new MessageBuilder();
			try (SaxDocumentBuilder saxBuilder = new SaxDocumentBuilder("text", messageBuilder.asXmlWriter(), false)) {
				for (int counter = 0; result[counter] != null; counter++) {
					saxBuilder.addElement("block", result[counter]);
				}
			}
			return new PipeRunResult(getSuccessForward(), messageBuilder.build());

		} catch (Exception e) {
			throw new PipeRunException(this, "Cannot create text blocks", e);
		}
	}

	/**
	 * Set the maximum number of characters of a block
	 * @ff.default 160
	 */
	public void setMaxBlockLength(int maxBlockLength) {
		this.maxBlockLength = maxBlockLength;
	}

	/**
	 * If {@code true}, try to break up the message at spaces, instead of in the middle of words
	 * @ff.default false
	 */
	public void setSoftSplit(boolean softSplit) {
		this.softSplit = softSplit;
	}
}
