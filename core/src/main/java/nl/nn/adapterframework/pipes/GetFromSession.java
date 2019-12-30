/*
   Copyright 2013, 2018 Nationale-Nederlanden

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

import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Gets the contents of the {@link IPipeLineSession pipeLineSession} by a key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>.
 *
 * @author Johan Verrips
 *
 * @see IPipeLineSession
 */

public class GetFromSession  extends FixedForwardPipe {

	private String sessionKey;
	private String type = null;

	@Override
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		String key = getSessionKey();
		if(StringUtils.isEmpty(key))
			key = (String) input;

		Object result = session.get(key);

		if (result == null) {
			//why is null returned when nothing can be found?
			log.warn(getLogPrefix(session)+"got null value from session under key ["+getSessionKey()+"]");
		}
		else {
			if (Parameter.TYPE_MAP.equals(getType()) && result instanceof Map) {
				Map<String, String> items = (Map<String, String>) result;
				XmlBuilder itemsXml = new XmlBuilder("items");
				for (Iterator<String> it = items.keySet().iterator(); it.hasNext();) {
					String item = it.next();
					XmlBuilder itemXml = new XmlBuilder("item");
					itemXml.addAttribute("name", item);
					itemXml.setValue(items.get(item));
					itemsXml.addSubElement(itemXml);
				}
				result = itemsXml.toXML();
			}
			log.debug(getLogPrefix(session) + "got [" + result.toString() + "] from pipeLineSession under key [" + getSessionKey() + "]");
		}

		return new PipeRunResult(getForward(), result);
	}

	/**
	 * Returns the name of the key in the {@link IPipeLineSession pipeLineSession} to retrieve the input from
	 */
	public String getSessionKey() {
		return sessionKey;
	}

	/**
	 * Sets the name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see IPipeLineSession
	 */
	@IbisDoc({"1", "Key of the session variable to retrieve the output message from. When left unspecified, the input message is used as the key of the session variable", ""})
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	@IbisDoc({"2", "<ul><li><code>string</code>: renders the contents</li><li><code>map</code>: converts a Map&lt;String, String&gt; object to a xml-string (&lt;items&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;/items&gt;)</li></ul>", "string"})
	public void setType(String type) {
		this.type = type;
	}
	public String getType() {
		return type;
	}


}
