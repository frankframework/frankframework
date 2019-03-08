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
import nl.nn.adapterframework.doc.IbisDoc;

/**
 * Puts the input in the PipeLineSession, under the key specified by
 * <code>{@link #setSessionKey(String) sessionKey}</code>.
 *
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
public class PutInSession extends FixedForwardPipe {
	
    private String sessionKey;
	private String value;
	
	/**
     * Checks whether the proper forward is defined.
     */
    public void configure() throws ConfigurationException {
	    super.configure();

        if (null== getSessionKey()) {
            throw new ConfigurationException("Pipe [" + getName() + "]"
                    + " has a null value for sessionKey");
        }
    }
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		Object v; 
		if (getValue() == null) {
			v = input;
		} else {
			v = value;
		}
		session.put(getSessionKey(), v);
		log.debug(getLogPrefix(session)+"stored ["+v.toString()+"] in pipeLineSession under key ["+getSessionKey()+"]");
		return new PipeRunResult(getForward(), input);
	}
	/**
	 * Gets the name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see IPipeLineSession
	 */
	public String getSessionKey() {
		return sessionKey;
	}
	/**
	 * Sets name of the key in the <code>PipeLineSession</code> to store the input in
	 * @see IPipeLineSession
	 */
	@IbisDoc({"name of the key in the <code>pipelinesession</code> to store the input in", ""})
	public void setSessionKey(String newSessionKey) {
		sessionKey = newSessionKey;
	}

	/**
	 * Sets value to store in the <code>PipeLineSession</code>
	 * @see IPipeLineSession
	 */
	@IbisDoc({"the value to store the in the <code>pipelinesession</code>. if not set, the input of the pipe is stored", ""})
	public void setValue(String value) {
		this.value = value;
	}
	public String getValue() {
		return value;
	}	
	
}
