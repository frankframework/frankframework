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
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.ElementType;
import org.frankframework.doc.ElementType.ElementTypes;
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
@ElementType(ElementTypes.TRANSLATOR)
public class EscapePipe extends FixedForwardPipe {

	private @Getter String substringStart;
	private @Getter String substringEnd;
	private @Getter Direction direction = Direction.ENCODE;
	private boolean encodeSubstring = false;

	public enum Direction {
		ENCODE, DECODE, CDATA2TEXT;
	}

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getDirection() == null) {
			throw new ConfigurationException("direction must be set");
		}
		if ((substringStart != null && substringEnd == null) || (substringStart == null && substringEnd != null)) {
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
		if (substringStart == null || substringEnd == null) {
			return handle(input);
		}

		int i = -1;
		while ((i = input.indexOf(substringStart, i + 1)) != -1) {
			int j = input.indexOf(substringEnd, i);
			if (j != -1) {
				// Pak de content tussen de substrings
				String content = input.substring(i + substringStart.length(), j);

				String processedContent = handle(content);

				// Replace the geparste content in de originele string
				input = input.substring(0, i + substringStart.length()) + processedContent + input.substring(j);
			} else {
				break;
			}
		}
		return input;
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
			throw new NotImplementedException("unknown direction ["+getDirection()+"]");
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
	 * @ff.default false
	 */
	public void setEncodeSubstring(boolean b) {
		encodeSubstring = b;
	}
}
