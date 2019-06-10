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
 * @author     Richard Punt / Gerrit van Brakel
 */
@IbisDescription(
	"Pipe that compares the integer values of two session variables." + 
	"Used to in combination with {@link IncreaseIntegerPipe} to contstruct loops." + 
	"<tr><td>{@link #setSessionKey2(String) sessionKey2}</td><td>reference to the other session variables to be compared</td><td></td></tr>" + 
	"</table>" + 
	"</p>" + 
	"<p><b>Exits:</b>" + 
	"<table border=\"1\">" + 
	"<tr><th>state</th><th>condition</th></tr>" + 
	"<tr><td>lessthan</td><td>when v1 &lt; v2</td></tr>" + 
	"<tr><td>greaterthan</td><td>when v1 &gt; v2</td></tr>" + 
	"<tr><td>equals</td><td>when v1 = v1</td></tr>" + 
	"</table>" + 
	"</p>" 
)
public class CompareIntegerPipe extends AbstractPipe {

	private final static String LESSTHANFORWARD = "lessthan";
	private final static String GREATERTHANFORWARD = "greaterthan";
	private final static String EQUALSFORWARD = "equals";

	private String sessionKey1 = null;
	private String sessionKey2 = null;

	public void configure() throws ConfigurationException {
		super.configure();

		if (StringUtils.isEmpty(sessionKey1))
			throw new ConfigurationException(getLogPrefix(null) + "sessionKey1 must be filled");

		if (StringUtils.isEmpty(sessionKey2))
			throw new ConfigurationException(getLogPrefix(null) + "sessionKey2 must be filled");

		if (null == findForward(LESSTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ LESSTHANFORWARD+ "] is not defined");

		if (null == findForward(GREATERTHANFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ GREATERTHANFORWARD+ "] is not defined");

		if (null == findForward(EQUALSFORWARD))
			throw new ConfigurationException(getLogPrefix(null)	+ "forward ["+ EQUALSFORWARD+ "] is not defined");
	}

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		String sessionKey1StringValue = (String) session.get(sessionKey1);
		String sessionKey2StringValue = (String) session.get(sessionKey2);

		if (log.isDebugEnabled()) {
			log.debug("sessionKey1StringValue [" + sessionKey1StringValue + "]");
			log.debug("sessionKey2StringValue [" + sessionKey2StringValue + "]");
		}

		Integer sessionKey1IntegerValue;
		Integer sessionKey2IntegerValue;
		try {
			sessionKey1IntegerValue = new Integer(sessionKey1StringValue);
			sessionKey2IntegerValue = new Integer(sessionKey2StringValue);
		} catch (Exception e) {
			PipeRunException prei =
				new PipeRunException(
					this, "Exception while comparing integers", e);
			throw prei;
		}

		int comparison=sessionKey1IntegerValue.compareTo(sessionKey2IntegerValue);
		if (comparison == 0)
			return new PipeRunResult(findForward(EQUALSFORWARD), input);
		else if (comparison < 0)
			return new PipeRunResult(findForward(LESSTHANFORWARD), input);
		else
			return new PipeRunResult(findForward(GREATERTHANFORWARD), input);

	}

	@IbisDoc({"reference to one of the session variables to be compared", ""})
	public void setSessionKey1(String string) {
		sessionKey1 = string;
	}
	public String getSessionKey1() {
		return sessionKey1;
	}

	@IbisDoc({"reference to the other session variables to be compared", ""})
	public void setSessionKey2(String string) {
		sessionKey2 = string;
	}
	public String getSessionKey2() {
		return sessionKey2;
	}

}
