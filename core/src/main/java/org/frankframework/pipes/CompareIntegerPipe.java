/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2021, 2023 WeAreFrank!

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

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * Pipe that compares the two integer values.
 * If one of the parameters is missing, then the input message will be used as the missing operand.
 * This pipe can be used in combination with {@link IncreaseIntegerPipe} to construct loops.
 *
 * @ff.parameter operand1 The first operand, holds v1.
 * @ff.parameter operand2 The second operand, holds v2.
 *
 * @author     Richard Punt / Gerrit van Brakel
 */
@Forward(name = "lessthan", description = "operand1 &lt; operand2")
@Forward(name = "greaterthan", description = "operand1 &gt; operand2")
@Forward(name = "equals", description = "operand1 = operand2")
@Category(Category.Type.BASIC)
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ROUTER)
public class CompareIntegerPipe extends AbstractPipe {

	private static final String LESSTHANFORWARD = "lessthan";
	private static final String GREATERTHANFORWARD = "greaterthan";
	private static final String EQUALSFORWARD = "equals";

	protected static final String OPERAND1 = "operand1";
	protected static final String OPERAND2 = "operand2";

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (null == findForward(LESSTHANFORWARD))
			throw new ConfigurationException("forward [" + LESSTHANFORWARD + "] is not defined");

		if (null == findForward(GREATERTHANFORWARD))
			throw new ConfigurationException("forward [" + GREATERTHANFORWARD + "] is not defined");

		if (null == findForward(EQUALSFORWARD))
			throw new ConfigurationException("forward [" + EQUALSFORWARD + "] is not defined");

		ParameterList parameterList = getParameterList();
		if (!parameterList.hasParameter(OPERAND1) && !parameterList.hasParameter(OPERAND2)) {
			throw new ConfigurationException("has neither parameter [" + OPERAND1 + "] nor parameter [" + OPERAND2 + "] specified");
		}
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl;
		try {
			pvl = getParameterList().getValues(message, session);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception extracting parameters", e);
		}
		Integer operand1 = getOperandValue(pvl, OPERAND1, message);
		Integer operand2 = getOperandValue(pvl, OPERAND2, message);

		int comparison = operand1.compareTo(operand2);
		if (comparison == 0)
			return new PipeRunResult(findForward(EQUALSFORWARD), message);
		else if (comparison < 0)
			return new PipeRunResult(findForward(LESSTHANFORWARD), message);
		else
			return new PipeRunResult(findForward(GREATERTHANFORWARD), message);

	}

	private Integer getOperandValue(ParameterValueList pvl, String operandName, Message message) throws PipeRunException {
		ParameterValue pv = pvl.get(operandName);
		Integer operand = null;
		if (pv != null && pv.getValue() != null) {
			operand = pv.asIntegerValue(0);
		}

		if (operand == null) {
			try {
				operand = Integer.valueOf(message.asString());
			} catch (Exception e) {
				throw new PipeRunException(this, "Exception on getting [" + operandName + "] from input message", e);
			}
		}
		return operand;
	}

	@Override
	public boolean consumesSessionVariable(String sessionKey) {
		return super.consumesSessionVariable(sessionKey);
	}

}
