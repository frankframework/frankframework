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

import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Gets the contents of the {@link nl.nn.adapterframework.core.IPipeLineSession pipeLineSession} by a key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>name of the key in the <code>PipeLineSession</code> to retrieve the output message from</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setType(String) type}</td><td>
 * <ul>
 * 	<li><code>string</code>: renders the contents</li>
 * 	<li><code>map</code>: converts a Map&lt;String, String&gt; object to a xml-string (&lt;items&gt;&lt;item name="..."&gt;...&lt;/item&gt;&lt;item name="..."&gt;...&lt;/item&gt;&lt;/items&gt;)</li>
 * </ul>
 * </td><td>string</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Johan Verrips
 *
 * @see IPipeLineSession
 */

 public class GetFromSession  extends FixedForwardPipe {

    private String sessionKey;
	private String type = null;

    /**
     * checks wether the proper forward is defined.
     * @throws ConfigurationException
     */
    public void configure() throws ConfigurationException {
	    super.configure();

        if (null== getSessionKey()) {
            throw new ConfigurationException("Pipe [" + getName() + "]"
                    + " has a null value for sessionKey");
        }
    }
/**
 * This is where the action takes place. Pipes may only throw a PipeRunException,
 * to be handled by the caller of this object.
 */
public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
	Object result=session.get(getSessionKey());
	
	if (result==null) {
		log.warn(getLogPrefix(session)+"got null value from session under key ["+getSessionKey()+"]");
		} else {
			if (Parameter.TYPE_MAP.equals(getType()) && result instanceof Map) {
				Map<String, String> items = (Map<String, String>) result;
				XmlBuilder itemsXml = new XmlBuilder("items");
				for (Iterator it = items.keySet().iterator(); it.hasNext();) {
					String item = (String) it.next();
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
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * {@link nl.nn.adapterframework.core.IPipeLineSession pipeLineSession}
 */
public String getSessionKey() {
	return sessionKey;
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * @see nl.nn.adapterframework.core.IPipeLineSession
 * 
 * @param newSessionKey String
 */
public void setSessionKey(String newSessionKey) {
	sessionKey = newSessionKey;
}

public String getType() {
	return type;
}

public void setType(String type) {
	this.type = type;
}
}
