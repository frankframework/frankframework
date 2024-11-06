/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlEncodingUtils;
import org.frankframework.util.XmlUtils;

import lombok.Getter;

/**
 * Pipe that performs translations between special characters and their xml equivalents.
 * <p>When direction=cdata2text all cdata nodes are converted to text nodes without any other translations.</p>
 *
 * @author Peter Leeuwenburgh
 */
@EnterpriseIntegrationPattern(Type.TRANSLATOR)
public class EscapePipe extends FixedForwardPipe {

	private @Getter String substringStart;
	private @Getter String substringEnd;
	private @Getter Direction direction = Direction.ENCODE;
	private boolean encodeSubstring = false;

	public enum Direction {
		ENCODE, DECODE, CDATA2TEXT
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getDirection() == null) {
			throw new ConfigurationException("direction must be set");
		}
		if ((StringUtils.isNotBlank(substringStart) && StringUtils.isBlank(substringEnd)) || (StringUtils.isBlank(substringStart) && StringUtils.isNotBlank(substringEnd))) {
			throw new ConfigurationException("cannot have only one of substringStart or substringEnd");
		}
		if (isEncodeSubstring()) {
			substringStart = XmlEncodingUtils.encodeChars(substringStart);
			substringEnd = XmlEncodingUtils.encodeChars(substringEnd);
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String input;
		try {
			input = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}

		String result = handleSubstrings(input);
		return new PipeRunResult(getSuccessForward(), result);
	}

	private String handleSubstrings(String input) {
		if (StringUtils.isBlank(substringStart) || StringUtils.isBlank(substringEnd)) {
			return handle(input);
		}

		StringBuilder result = new StringBuilder(input.length());
		int startIndex = 0;

		while (startIndex < input.length()) {
			// Find the index of substringStart in the remaining part of the input
			int start = input.indexOf(substringStart, startIndex);

			// If substringStart is not found, append the remaining part and exit the loop
			if (start == -1) {
				result.append(input, startIndex, input.length());
				break;
			}

			// Find the index of substringEnd after the current substringStart
			int end = input.indexOf(substringEnd, start + substringStart.length());

			// If substringEnd is not found, append the remaining part and exit the loop
			if (end == -1) {
				result.append(input, startIndex, input.length());
				break;
			}

			// Append the part from the last startIndex to the current substringStart
			result.append(input, startIndex, start + substringStart.length());

			String content = input.substring(start + substringStart.length(), end);

			String handledContent = handle(content);
			handledContent = handledContent == null ? "null" : handledContent;

			result.append(handledContent);

			startIndex = end;
		}
		return result.toString();
	}

	private String handle(String input) {
		switch (getDirection()) {
			case ENCODE:
				return XmlEncodingUtils.encodeChars(input);
			case DECODE:
				return XmlEncodingUtils.decodeChars(input);
			case CDATA2TEXT:
				return XmlUtils.cdataToText(input);
			default:
				throw new NotImplementedException("unknown direction [" + getDirection() + "]");
		}
	}

	// ESCAPE BETWEEN

	/** substring to start translation */
	public void setSubstringStart(String substringStart) {
		this.substringStart = substringStart;
	}

	/** substring to end translation */
	public void setSubstringEnd(String substringEnd) {
		this.substringEnd = substringEnd;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	public boolean isEncodeSubstring() {
		return encodeSubstring;
	}

	/**
	 * when set <code>true</code> special characters in <code>substringstart</code> and <code>substringend</code> are first translated to their xml equivalents
	 *
	 * @ff.default false
	 */
	public void setEncodeSubstring(boolean b) {
		encodeSubstring = b;
	}
}
