/*
   Copyright 2013 Nationale-Nederlanden

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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe that performs translations between special characters and their xml equivalents.
 * <p>When direction=cdata2text all cdata nodes are converted to text nodes without any other translations.</p>
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDirection(String) direction}</td><td>either <code>encode</code>, <code>decode</code> or <code>cdata2text</code></td><td>encode</td></tr>
 * <tr><td>{@link #setSubstringStart(String) substringStart}</td><td>substring to start translation</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSubstringEnd(String) substringEnd}</td><td>substring to end translation</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setEncodeSubstring(boolean) decodeSubstring}</td><td>when set <code>true</code> special characters in <code>substringStart</code> and <code>substringEnd</code> are first translated to their xml equivalents</td><td>false</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @version $Id$
 * @author Peter Leeuwenburgh
 */
public class EscapePipe extends FixedForwardPipe {

	private String substringStart;
	private String substringEnd;
	private String direction = "encode";
	private boolean encodeSubstring = false;


	public void configure() throws ConfigurationException {
		super.configure();
		String dir = getDirection();
		if (dir == null) {
			throw new ConfigurationException(
				getLogPrefix(null) + "direction must be set");
		}
		if (!dir.equalsIgnoreCase("encode")
			&& !dir.equalsIgnoreCase("decode")
				&& !dir.equalsIgnoreCase("cdata2text")) {
			throw new ConfigurationException(
				getLogPrefix(null)
					+ "illegal value for direction ["
					+ dir
					+ "], must be 'encode', 'decode' or 'cdata2text'");
		}
		if ((substringStart != null && substringEnd == null)
			|| (substringStart == null && substringEnd != null)) {
			throw new ConfigurationException(
				getLogPrefix(null)
					+ "cannot have only one of substringStart or substringEnd");
		}
		if (isEncodeSubstring()) {
			substringStart = XmlUtils.encodeChars(substringStart);
			substringEnd = XmlUtils.encodeChars(substringEnd);
		}
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		String string = input.toString();
		String substring = null;
		String result = string;
		int i = -1;
		int j = -1;
		log.debug("string [" + string + "]");
		log.debug("substringStart [" + substringStart + "]");
		log.debug("substringEnd [" + substringEnd + "]");
		if (substringStart != null && substringEnd != null) {
			i = string.indexOf(substringStart);
			if (i != -1) {
				j = string.indexOf(substringEnd, i);
				if (j != -1) {
					substring =
						string.substring(i + substringStart.length(), j);
					if ("encode".equalsIgnoreCase(getDirection())) {
						substring = XmlUtils.encodeChars(substring);
					} else {
						if ("decode".equalsIgnoreCase(getDirection())) {
							substring = XmlUtils.decodeChars(substring);
						} else {
							substring = XmlUtils.cdataToText(substring);
						}
					}
					result =
						string.substring(0, i + substringStart.length())
							+ substring
							+ string.substring(j);
				}
			}
		} else {
			if ("encode".equalsIgnoreCase(getDirection())) {
				result = XmlUtils.encodeChars(string);
			} else {
				if ("decode".equalsIgnoreCase(getDirection())) {
					result = XmlUtils.decodeChars(string);
				} else {
					result = XmlUtils.cdataToText(string);
				}
			}
		}

		return new PipeRunResult(getForward(), result);
	}

	public String getSubstringStart() {
		return substringStart;
	}
	public void setSubstringStart(String substringStart) {
		this.substringStart = substringStart;
	}

	public String getSubstringEnd() {
		return substringEnd;
	}
	public void setSubstringEnd(String substringEnd) {
		this.substringEnd = substringEnd;
	}

	public String getDirection() {
		return direction;
	}
	public void setDirection(String direction) {
		this.direction = direction;
	}

	public boolean isEncodeSubstring() {
		return encodeSubstring;
	}
	public void setEncodeSubstring(boolean b) {
		encodeSubstring = b;
	}
}