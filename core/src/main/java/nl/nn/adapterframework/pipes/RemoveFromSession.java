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

import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import nl.nn.adapterframework.doc.IbisDoc;
import org.apache.commons.lang.StringUtils;

/**
 * Removes a key specified by <code>{@link #setSessionKey(String) sessionKey}</code>
 * from the {@link IPipeLineSession pipeLineSession}.
 *
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * @author Peter Leeuwenburgh
 *
 * @see IPipeLineSession
 */

 public class RemoveFromSession  extends FixedForwardPipe {
    private String sessionKey;

	public RemoveFromSession() {
		super.setPreserveInput(true);
	}
    
	/**
     * checks wether the proper forward is defined.
     * @throws ConfigurationException
     */
    public void configure() throws ConfigurationException {
	    super.configure();

	/*
        if (null== getSessionKey()) {
            throw new ConfigurationException("Pipe [" + getName() + "]"
                    + " has a null value for sessionKey");
        }
	*/
    }
/**
 * This is where the action takes place. Pipes may only throw a PipeRunException,
 * to be handled by the caller of this object.
 */
public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
	String result = null;

	String sessionKeys = getSessionKey();
	if (StringUtils.isEmpty(sessionKeys)) {
		sessionKeys = (String)input;
	}
	if (StringUtils.isEmpty(sessionKeys)) {
		log.warn(getLogPrefix(session)+"no key specified");
		result="[null]";
	} else {
		StringTokenizer st = new StringTokenizer(sessionKeys, ",");
		while (st.hasMoreElements()) {
			String sk = st.nextToken();
			Object skResult = session.remove(sk);
			if (skResult==null) {
				log.warn(getLogPrefix(session)+"key ["+sk+"] not found");
				skResult="[null]";
			} else {
				log.debug(getLogPrefix(session) +"key ["+sk+"] removed");
			}
			if (result == null) {
				result = (String)skResult;
			} else {
				result = result + "," + skResult;
			}
		}
	}
	
	return new PipeRunResult(getForward(), result);
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * {@link IPipeLineSession pipeLineSession}
 */
public String getSessionKey() {
	return sessionKey;
}
/**
 * The name of the key in the <code>PipeLineSession</code> to store the input in
 * @see IPipeLineSession
 * 
 * @param newSessionKey String
 */
	@IbisDoc({"name of the key in the <code>pipelinesession</code> to remove. if this key is empty the input message is interpretted as key. for multiple keys use ',' as delimiter", ""})
public void setSessionKey(String newSessionKey) {
	sessionKey = newSessionKey;
}
}
