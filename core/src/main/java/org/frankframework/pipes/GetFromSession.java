/*
   Copyright 2013, 2018, 2020 Nationale-Nederlanden

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
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.EnterpriseIntegrationPattern.Type;
import org.frankframework.parameters.ParameterType;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlBuilder;

/**
 * Gets the contents of the {@link PipeLineSession pipeLineSession} by a key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>.
 *
 * @author Johan Verrips
 *
 * @see PipeLineSession
 */
@EnterpriseIntegrationPattern(Type.SESSION)
public class GetFromSession  extends FixedForwardPipe {

	private String sessionKey;
	private ParameterType type = null;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String key = getSessionKey();
		if(StringUtils.isEmpty(key)) {
			try {
				key = message.asString();
			} catch (IOException e) {
				throw new PipeRunException(this, "cannot open stream", e);
			}
		}

		Object result = session.get(key);

		if (result == null) {
			//why is null returned when nothing can be found?
			log.warn("got null value from session under key [{}]", getSessionKey());
		}
		else {
			if (getType()==ParameterType.MAP && result instanceof Map) {
				Map<String, String> items = (Map<String, String>) result;
				XmlBuilder itemsXml = new XmlBuilder("items");
				for (Iterator<String> it = items.keySet().iterator(); it.hasNext();) {
					String item = it.next();
					XmlBuilder itemXml = new XmlBuilder("item");
					itemXml.addAttribute("name", item);
					itemXml.setValue(items.get(item));
					itemsXml.addSubElement(itemXml);
				}
				result = itemsXml.asMessage();
			}
			log.debug("got [{}] from pipeLineSession under key [{}]", result, getSessionKey());
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	/**
	 * Returns the name of the key in the {@link PipeLineSession pipeLineSession} to retrieve the input from
	 */
	public String getSessionKey() {
		return sessionKey;
	}

	/** Key of the session variable to retrieve the output message from. When left unspecified, the input message is used as the key of the session variable */
	public void setSessionKey(String sessionKey) {
		this.sessionKey = sessionKey;
	}

	/**
	 * <ul><li><code>string</code>: renders the contents</li><li><code>map</code>: converts a Map&lt;String, String&gt; object to a xml-string (&lt;items&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;item name='...'&gt;...&lt;/item&gt;&lt;/items&gt;)</li></ul>
	 * @ff.default string
	 */
	public void setType(ParameterType type) {
		this.type = type;
	}
	public ParameterType getType() {
		return type;
	}


}
