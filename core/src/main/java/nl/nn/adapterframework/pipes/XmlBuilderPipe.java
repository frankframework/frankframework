/*
   Copyright 2015 Nationale-Nederlanden

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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * Pipe for converting special characters to their xml equivalents. 
 *
 * 
 * @author Peter Leeuwenburgh
 */
public class XmlBuilderPipe extends FixedForwardPipe {

	private String substringStart;
	private String substringEnd;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		String result = input.toString();
		if (getSubstringStart() != null && getSubstringEnd() != null) {
			int i = result.indexOf(getSubstringStart());
			while (i != -1
					&& result.length() > i + getSubstringStart().length()) {
				int j = result.indexOf(getSubstringEnd(), i
						+ getSubstringStart().length());
				if (j != -1) {
					String xml = result.substring(i
							+ getSubstringStart().length(), j);
					xml = buildXml(xml);
					result = result.substring(0, i) + getSubstringStart() + xml
							+ result.substring(j);
					i = result.indexOf(getSubstringStart(), i
							+ getSubstringStart().length() + xml.length()
							+ getSubstringEnd().length());
				} else {
					i = -1;
				}
			}
		}
		return new PipeRunResult(getForward(), result);
	}

	private String buildXml(String xml) {
		String result = XmlUtils.decodeChars(xml);
		if (XmlUtils.isWellFormed(result)) {
			result = XmlUtils.removeNamespaces(result);
		} else {
			return xml;
		}
		return result;
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
}