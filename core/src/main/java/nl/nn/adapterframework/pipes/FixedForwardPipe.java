/*
   Copyright 2013, 2016, 2020 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
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

	/**
	 * checks for correct configuration of forward
	 */
	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		successForward = findForward(PipeForward.SUCCESS_FORWARD_NAME);
		if (successForward == null)
			throw new ConfigurationException("has no forward with name [" + PipeForward.SUCCESS_FORWARD_NAME + "]");
	}

	/**
	 * called by {@link InputOutputPipeProcessor} to check if the pipe needs to be skipped.
	 */
	public PipeRunResult doInitialPipe(Message input, PipeLineSession session) throws PipeRunException {
		if (isSkipOnEmptyInput() && (input == null || StringUtils.isEmpty(input.toString()))) {
			return new PipeRunResult(getSuccessForward(), input);
		}
		if (StringUtils.isNotEmpty(getIfParam())) {
			boolean skipPipe = true;

			ParameterValueList pvl = null;
			if (getParameterList() != null) {
				try {
					pvl = getParameterList().getValues(input, session);
				} catch (ParameterException e) {
					throw new PipeRunException(this, getLogPrefix(session) + "exception on extracting parameters", e);
				}
			}
			String ip = getParameterValue(pvl, getIfParam());
			if (ip == null) {
				if (getIfValue() == null) {
					skipPipe = false;
				}
			} else {
				if (getIfValue() != null && getIfValue().equalsIgnoreCase(ip)) {
					skipPipe = false;
				}
			}
			if (skipPipe) {
				return new PipeRunResult(getSuccessForward(), input);
			}
		}
		return null;
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

	@IbisDoc({"If set, this pipe is only executed when the value of parameter with name <code>ifparam</code> equals <code>ifvalue</code> (otherwise this pipe is skipped)", ""})
	public void setIfParam(String string) {
		ifParam = string;
	}

	@IbisDoc({"See <code>ifparam</code>", ""})
	public void setIfValue(String string) {
		ifValue = string;
	}
}
