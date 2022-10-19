/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020-2022 WeAreFrank!

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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.processors.InputOutputPipeProcessor;
import nl.nn.adapterframework.stream.Message;

/**
 * Provides a base-class for a Pipe that always has the same forward.
 * Ancestor classes should call <code>super.configure()</code> in their <code>configure()</code>-methods.
 *
 * @author Gerrit van Brakel
 */
public abstract class FixedForwardPipe extends AbstractPipe {

	private @Getter PipeForward successForward;
	private @Getter boolean skipOnEmptyInput = false;
	private @Getter String ifParam = null;
	private @Getter String ifValue = null;

	private @Getter String onlyIfSessionKey;
	private @Getter String onlyIfValue;
	private @Getter String unlessSessionKey;
	private @Getter String unlessValue;

	private Parameter ifParameter = null;

	/**
	 * checks for correct configuration of forward
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		successForward = findForward(PipeForward.SUCCESS_FORWARD_NAME);
		if (successForward == null) {
			throw new ConfigurationException("has no forward with name [" + PipeForward.SUCCESS_FORWARD_NAME + "]");
		}
		if (StringUtils.isNotEmpty(getIfParam())) {
			if (getParameterList() != null) {
				ifParameter = getParameterList().findParameter(getIfParam());
			}
			if (ifParameter==null) {
				ConfigurationWarnings.add(this, log, "ifParam ["+getIfParam()+"] not found");
			}
		}
	}

	/**
	 * called by {@link InputOutputPipeProcessor} to check if the pipe needs to be skipped.
	 */
	public boolean skipPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (isSkipOnEmptyInput() && Message.isEmpty(input)) {
			return true;
		}
		if (StringUtils.isNotEmpty(getOnlyIfSessionKey())) {
			Object onlyIfActualValue = session.get(getOnlyIfSessionKey());
			if (onlyIfActualValue==null || StringUtils.isNotEmpty(getOnlyIfValue()) && !getOnlyIfValue().equals(onlyIfActualValue)) {
				log.debug("onlyIfSessionKey [{}] value [{}]: not found or not equal to value [{}]", getOnlyIfSessionKey(), onlyIfActualValue, getOnlyIfValue());
				return true;
			}
		}
		if (StringUtils.isNotEmpty(getUnlessSessionKey())) {
			Object unlessActualValue = session.get(getUnlessSessionKey());
			if (unlessActualValue!=null && (StringUtils.isEmpty(getUnlessValue()) || getUnlessValue().equals(unlessActualValue))) {
				log.debug("unlessSessionKey [{}] value [{}]: not found or equal to value [{}]", getUnlessSessionKey(), unlessActualValue, getUnlessValue());
				return true;
			}
		}
		try {
			if (ifParameter!=null) {
				Object paramValue = ifParameter.getValue(null, input, session, true);
				String ifValue = getIfValue();
				if (ifValue == null) {
					return paramValue!=null;
				}
				return !ifValue.equalsIgnoreCase(Message.asString(paramValue));
			}
		} catch (ParameterException | IOException e) {
			throw new PipeRunException(this, "Cannot evaluate ifParam", e);
		}
		return false;
	}

	protected String getParameterValue(ParameterValueList pvl, String parameterName) {
		ParameterList parameterList = getParameterList();
		if (pvl != null && parameterList != null) {
			ParameterValue pv = pvl.findParameterValue(parameterName);
			if(pv != null) {
				return pv.asStringValue(null);
			}
		}
		return null;
	}

	@IbisDoc({"If set, the processing continues directly at the forward of this pipe, without executing the pipe itself, if the input is empty", "false"})
	public void setSkipOnEmptyInput(boolean b) {
		skipOnEmptyInput = b;
	}

	@IbisDoc({"If set, this pipe is only executed when the value of parameter with name <code>ifParam</code> equals <code>ifValue</code> (otherwise this pipe is skipped)", ""})
	public void setIfParam(String string) {
		ifParam = string;
	}

	@IbisDoc({"See <code>ifParam</code>", ""})
	public void setIfValue(String string) {
		ifValue = string;
	}

	@IbisDoc({"Key of session variable to check if action must be executed. The pipe is only executed if the session variable exists and is not null", ""})
	public void setOnlyIfSessionKey(String onlyIfSessionKey) {
		this.onlyIfSessionKey = onlyIfSessionKey;
	}

	@IbisDoc({"Value of session variable 'onlyIfSessionKey' to check if action must be executed. The pipe is only executed if the session variable has the specified value", ""})
	public void setOnlyIfValue(String onlyIfValue) {
		this.onlyIfValue = onlyIfValue;
	}

	@IbisDoc({"Key of session variable to check if action must be executed. The pipe is not executed if the session variable exists and is not null", ""})
	public void setUnlessSessionKey(String unlessSessionKey) {
		this.unlessSessionKey = unlessSessionKey;
	}

	@IbisDoc({"Value of session variable 'unlessSessionKey' to check if action must be executed. The pipe is not executed if the session variable has the specified value", ""})
	public void setUnlessValue(String unlessValue) {
		this.unlessValue = unlessValue;
	}

}
