/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020 WeAreFrank!

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
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

import org.apache.commons.lang3.StringUtils;

/**
 * Pipe that compares the two integer values read from {@link Parameter the parameters} <code>operand1</code> and <code>operand2</code>.
 * If one of the parameters is missing then the input message will be used as the missing operand.
 * This pipe can be used in combination with {@link IncreaseIntegerPipe} to construct loops.
 *
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>lessthan</td><td>when v1 &lt; v2</td></tr>
 * <tr><td>greaterthan</td><td>when v1 &gt; v2</td></tr>
 * <tr><td>equals</td><td>when v1 = v2</td></tr>
 * </table>
 * </p>
 * @author     Richard Punt / Gerrit van Brakel
 */
public class CompareIntegerPipe extends AbstractPipe {

	private final static String LESSTHANFORWARD = "lessthan";
	private final static String GREATERTHANFORWARD = "greaterthan";
	private final static String EQUALSFORWARD = "equals";

	private final static String OPERAND1 = "operand1";
	private final static String OPERAND2 = "operand2";

	private String sessionKey1 = null;
	private String sessionKey2 = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (null == findForward(LESSTHANFORWARD))
			throw new ConfigurationException("forward ["+ LESSTHANFORWARD+ "] is not defined");

		if (null == findForward(GREATERTHANFORWARD))
			throw new ConfigurationException("forward ["+ GREATERTHANFORWARD+ "] is not defined");

		if (null == findForward(EQUALSFORWARD))
			throw new ConfigurationException("forward ["+ EQUALSFORWARD+ "] is not defined");

		if (StringUtils.isEmpty(getSessionKey1()) && StringUtils.isEmpty(getSessionKey2())) {
			ParameterList parameterList = getParameterList();
			if (parameterList.findParameter(OPERAND1) == null && parameterList.findParameter(OPERAND2) == null) {
				throw new ConfigurationException("has neither parameter [" + OPERAND1 + "] nor parameter [" + OPERAND2 + "] specified");
			}
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}
		Integer operand1 = getOperandValue(pvl, OPERAND1, getSessionKey1(), message, session);
		Integer operand2 = getOperandValue(pvl, OPERAND2, getSessionKey2(), message, session);

		int comparison=operand1.compareTo(operand2);
		if (comparison == 0)
			return new PipeRunResult(findForward(EQUALSFORWARD), message);
		else if (comparison < 0)
			return new PipeRunResult(findForward(LESSTHANFORWARD), message);
		else
			return new PipeRunResult(findForward(GREATERTHANFORWARD), message);

	}

	private Integer getOperandValue(ParameterValueList pvl, String operandName, String sessionkey, Message message, PipeLineSession session) throws PipeRunException {
		ParameterValue pv = pvl.getParameterValue(operandName);
		Integer operand = null;
		if(pv != null && pv.getValue() != null) {
			operand = pv.asIntegerValue(0);
		}

		if (operand == null) {
			if (StringUtils.isNotEmpty(sessionkey)) {
				try {
					operand = Integer.parseInt(session.get(sessionkey)+"");
				} catch (Exception e) {
					throw new PipeRunException(this, getLogPrefix(session) + " Exception on getting [" + operandName + "] from session key ["+sessionkey+"]", e);
				}
			}
			if (operand == null) {
				try {
					operand = new Integer(message.asString());
				} catch (Exception e) {
					throw new PipeRunException(this, getLogPrefix(session) + " Exception on getting [" + operandName + "] from input message", e);
				}
			}
		}
		return operand;
	}

	@Deprecated
	@IbisDoc({"reference to one of the session variables to be compared", ""})
	@ConfigurationWarning("Please use the parameter operand1")
	public void setSessionKey1(String string) {
		sessionKey1 = string;
	}
	public String getSessionKey1() {
		return sessionKey1;
	}

	@Deprecated
	@IbisDoc({"reference to the other session variables to be compared", ""})
	@ConfigurationWarning("Please use the parameter operand2")
	public void setSessionKey2(String string) {
		sessionKey2 = string;
	}
	public String getSessionKey2() {
		return sessionKey2;
	}

}
