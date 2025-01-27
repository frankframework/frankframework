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
package org.frankframework.pipes;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.doc.Forward;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.processors.InputOutputPipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.util.MessageUtils;

/**
 * Provides a base-class for a Pipe that always has the same forward.
 * Ancestor classes should call <code>super.configure()</code> in their <code>configure()</code>-methods.
 *
 * @author Gerrit van Brakel
 */
@Forward(name = "success", description = "successful processing of the message of the pipe")
public abstract class FixedForwardPipe extends AbstractPipe {

	private @Getter PipeForward successForward;
	private @Getter boolean skipOnEmptyInput = false;
	private @Getter String ifParam = null;
	private @Getter String ifValue = null;

	private @Getter String onlyIfSessionKey;
	private @Getter String onlyIfValue;
	private @Getter String unlessSessionKey;
	private @Getter String unlessValue;

	private IParameter ifParameter = null;

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
			log.debug("skip pipe processing: empty input");
			return true;
		}
		if (StringUtils.isNotEmpty(getOnlyIfSessionKey())) {
			Object onlyIfActualValue = session.get(getOnlyIfSessionKey());
			if (onlyIfActualValue==null || StringUtils.isNotEmpty(getOnlyIfValue()) && !getOnlyIfValue().equals(onlyIfActualValue)) {
				log.debug("skip pipe processing: onlyIfSessionKey [{}] value [{}] not found or not equal to value [{}]", getOnlyIfSessionKey(), onlyIfActualValue, getOnlyIfValue());
				return true;
			}
		}
		if (StringUtils.isNotEmpty(getUnlessSessionKey())) {
			Object unlessActualValue = session.get(getUnlessSessionKey());
			if (unlessActualValue!=null && (StringUtils.isEmpty(getUnlessValue()) || getUnlessValue().equals(unlessActualValue))) {
				log.debug("skip pipe processing: unlessSessionKey [{}] value [{}] not found or equal to value [{}]", getUnlessSessionKey(), unlessActualValue, getUnlessValue());
				return true;
			}
		}
		try {
			if (ifParameter!=null) {
				Object paramValue = ifParameter.getValue(null, input, session, true);
				if (ifValue == null) {
					boolean paramValueIsNotNull = paramValue != null;
					log.debug("skip pipe processing: ifValue not set and ifParameter value [{}] not null", paramValue);
					return paramValueIsNotNull;
				}

				boolean ifValueNotEqualToIfParam = !ifValue.equalsIgnoreCase(MessageUtils.asString(paramValue));
				log.debug("skip pipe processing: ifValue value [{}] not equal to ifParameter value [{}]", ifValue, paramValue);
				return ifValueNotEqualToIfParam;
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

	/**
	 * If set, the processing continues directly at the forward of this pipe, without executing the pipe itself, if the input is empty
	 * @ff.default false
	 */
	public void setSkipOnEmptyInput(boolean b) {
		skipOnEmptyInput = b;
	}

	/** If set, this pipe is only executed when the value of parameter with name <code>ifParam</code> equals <code>ifValue</code> (otherwise this pipe is skipped) */
	public void setIfParam(String string) {
		ifParam = string;
	}

	/** See <code>ifParam</code> */
	public void setIfValue(String string) {
		ifValue = string;
	}

	/** Key of session variable to check if action must be executed. The pipe is only executed if the session variable exists and is not null */
	public void setOnlyIfSessionKey(String onlyIfSessionKey) {
		this.onlyIfSessionKey = onlyIfSessionKey;
	}

	/** Value of session variable 'onlyIfSessionKey' to check if action must be executed. The pipe is only executed if the session variable has the specified value */
	public void setOnlyIfValue(String onlyIfValue) {
		this.onlyIfValue = onlyIfValue;
	}

	/** Key of session variable to check if action must be executed. The pipe is not executed if the session variable exists and is not null */
	public void setUnlessSessionKey(String unlessSessionKey) {
		this.unlessSessionKey = unlessSessionKey;
	}

	/** Value of session variable 'unlessSessionKey' to check if action must be executed. The pipe is not executed if the session variable has the specified value */
	public void setUnlessValue(String unlessValue) {
		this.unlessValue = unlessValue;
	}

}
