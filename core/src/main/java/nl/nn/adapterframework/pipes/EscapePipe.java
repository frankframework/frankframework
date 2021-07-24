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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe that performs translations between special characters and their xml equivalents.
 * <p>When direction=cdata2text all cdata nodes are converted to text nodes without any other translations.</p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * </table>
 * </p>
 * @author Peter Leeuwenburgh
 */
public class EscapePipe extends FixedForwardPipe {

	private String substringStart;
	private String substringEnd;
	private String direction = "encode";
	private boolean encodeSubstring = false;


	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String dir = getDirection();
		if (dir == null) {
			throw new ConfigurationException("direction must be set");
		}
		if (!dir.equalsIgnoreCase("encode")
			&& !dir.equalsIgnoreCase("decode")
				&& !dir.equalsIgnoreCase("cdata2text")) {
			throw new ConfigurationException("illegal value for direction [" + dir + "], must be 'encode', 'decode' or 'cdata2text'");
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
		log.debug("input [" + input + "]");
		log.debug("substringStart [" + substringStart + "]");
		log.debug("substringEnd [" + substringEnd + "]");
		if (substringStart != null && substringEnd != null) {
			i = input.indexOf(substringStart);
			if (i != -1) {
				j = input.indexOf(substringEnd, i);
				if (j != -1) {
					substring = input.substring(i + substringStart.length(), j);
					if ("encode".equalsIgnoreCase(getDirection())) {
						substring = XmlUtils.encodeChars(substring);
					} else {
						if ("decode".equalsIgnoreCase(getDirection())) {
							substring = XmlUtils.decodeChars(substring);
						} else {
							substring = XmlUtils.cdataToText(substring);
						}
					}
					result = input.substring(0, i + substringStart.length()) + substring + input.substring(j);
				}
			}
		} else {
			if ("encode".equalsIgnoreCase(getDirection())) {
				result = XmlUtils.encodeChars(input);
			} else {
				if ("decode".equalsIgnoreCase(getDirection())) {
					result = XmlUtils.decodeChars(input);
				} else {
					result = XmlUtils.cdataToText(input);
				}
			}
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	public String getSubstringStart() {
		return substringStart;
	}

	@IbisDoc({"substring to start translation", ""})
	public void setSubstringStart(String substringStart) {
		this.substringStart = substringStart;
	}

	public String getSubstringEnd() {
		return substringEnd;
	}

	@IbisDoc({"substring to end translation", ""})
	public void setSubstringEnd(String substringEnd) {
		this.substringEnd = substringEnd;
	}

	public String getDirection() {
		return direction;
	}

	@IbisDoc({"either <code>encode</code>, <code>decode</code> or <code>cdata2text</code>", "encode"})
	public void setDirection(String direction) {
		this.direction = direction;
	}

	public boolean isEncodeSubstring() {
		return encodeSubstring;
	}

	@IbisDoc({"when set <code>true</code> special characters in <code>substringstart</code> and <code>substringend</code> are first translated to their xml equivalents", "false"})
	public void setEncodeSubstring(boolean b) {
		encodeSubstring = b;
	}
}