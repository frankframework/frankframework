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
import nl.nn.adapterframework.doc.IbisDescription; 
import org.apache.commons.lang.StringUtils;


/** 
 * @author Richard Punt / Gerrit van Brakel
 */
@IbisDescription(
	"Pipe that increases the integer values of a session variable." + 
	"Used to in combination with {@link CompareIntegerPipe} to contstruct loops." + 
	"<tr><td>{@link #setSessionKey(String) sessionKey}</td><td>reference to the session variable whose value is to be increased</td><td></td></tr>" + 
	"<tr><td>{@link #setIncrement(int) increment}</td><td>amount to increment the value</td><td>1</td></tr>" + 
	"</table>" + 
	"</p>" + 
	"<p><b>Exits:</b>" + 
	"<table border=\"1\">" + 
	"<tr><th>state</th><th>condition</th></tr>" + 
	"<tr><td>\"success\"</td><td>default</td></tr>" + 
	"<tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>" + 
	"</table>" + 
	"</p>" 
)
public class IncreaseIntegerPipe extends FixedForwardPipe {

	private String sessionKey=null;
	private int increment=1;

	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(sessionKey))
			throw new ConfigurationException(getLogPrefix(null)+"sessionKey must be filled");
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		String sessionKeyString = (String) session.get(sessionKey);
		Integer sessionKeyInteger = Integer.valueOf(sessionKeyString);
		session.put(sessionKey, sessionKeyInteger.intValue() + increment + "");

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix(session)+"stored ["+session.get(sessionKey)+"] in pipeLineSession under key ["+getSessionKey()+"]");
		}
		return new PipeRunResult(findForward("success"), input);
	}

	@IbisDoc({"reference to the session variable whose value is to be increased", ""})
	public void setSessionKey(String string) {
		sessionKey = string;
	}
	public String getSessionKey() {
		return sessionKey;
	}

	@IbisDoc({"amount to increment the value", "1"})
	public void setIncrement(int i) {
		increment = i;
	}
	public int getIncrement() {
		return increment;
	}

}
