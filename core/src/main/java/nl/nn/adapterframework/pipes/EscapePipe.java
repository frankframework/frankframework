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
package nl.nn.adapterframework.pipes;

import java.io.IOException;

import org.apache.commons.lang3.NotImplementedException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;

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
			substringStart = XmlUtils.encodeChars(substringStart);
			substringEnd = XmlUtils.encodeChars(substringEnd);
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

		String substring = null;
		String result = input;
		int i = -1;
		int j = -1;
		log.debug("substringStart [{}] substringEnd [{}] input [{}]", substringStart, substringEnd, input);
		if (substringStart != null && substringEnd != null) {
			i = input.indexOf(substringStart);
			if (i != -1) {
				j = input.indexOf(substringEnd, i);
				if (j != -1) {
					substring = input.substring(i + substringStart.length(), j);
					substring = handle(substring);
					result = input.substring(0, i + substringStart.length()) + substring + input.substring(j);
				}
			}
		} else {
			result = handle(input);
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	private String handle(String input) {
		switch (getDirection()) {
		case ENCODE:
			return XmlUtils.encodeChars(input);
		case DECODE:
			return XmlUtils.decodeChars(input);
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