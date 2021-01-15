/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

/**
 * Pipe that increases the integer values of a session variable.
 * Can be used in combination with {@link CompareIntegerPipe} to construct loops.
 * 
 * <p>
 * <table border="1">
 * <tr><td>{@link #setSessionKey(String) sessionKey}</td><td>reference to the session variable whose value is to be increased</td><td></td></tr>
 * <tr><td>{@link #setIncrement(int) increment}</td><td>amount to increment the value. Can be set from the attribute or the parameter 'increment'</td><td>1</td></tr>
 * </table>
 * </p>
 * @author Richard Punt / Gerrit van Brakel
 */
public class IncreaseIntegerPipe extends FixedForwardPipe {

	private String sessionKey=null;
	private int increment=1;
	private final static String PARAMETER_INCREMENT = "increment";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(sessionKey))
			throw new ConfigurationException("sessionKey must be filled");
	}

	@Override
	public PipeRunResult doPipe(Message message, IPipeLineSession session) throws PipeRunException {

		String sessionKeyString = (String) session.get(sessionKey);
		Integer sessionKeyInteger = Integer.valueOf(sessionKeyString);

		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}
		ParameterValue pv = pvl.getParameterValue(PARAMETER_INCREMENT);
		if(pv != null && pv.getValue() != null) {
			increment = pv.asIntegerValue(increment);
		}

		session.put(sessionKey, sessionKeyInteger.intValue() + increment + "");

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix(session)+"stored ["+session.get(sessionKey)+"] in pipeLineSession under key ["+getSessionKey()+"]");
		}
		return new PipeRunResult(findForward("success"), message);
	}

	@IbisDoc({"reference to the session variable whose value is to be increased", ""})
	public void setSessionKey(String string) {
		sessionKey = string;
	}
	public String getSessionKey() {
		return sessionKey;
	}

	@IbisDoc({"amount to increment the value. Can be set from the attribute or the parameter 'increment'", "1"})
	public void setIncrement(int i) {
		increment = i;
	}
	public int getIncrement() {
		return increment;
	}

}
