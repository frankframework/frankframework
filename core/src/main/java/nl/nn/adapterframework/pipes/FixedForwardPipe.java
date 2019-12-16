/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.processors.InputOutputPipeProcessor;

/**
 * Provides a base-class for a Pipe that always has the same forward.
 * Ancestor classes should call <code>super.configure()</code> in their <code>configure()</code>-methods.
 *
 * <tr><td>{@link #setSkipOnEmptyInput(boolean) skipOnEmptyInput}</td><td>when set, this pipe is skipped</td><td>false</td></tr>
 * <tr><td>{@link #setIfParam(String) ifParam}</td><td>when set, this pipe is only executed when the value of parameter with name <code>ifParam</code> equals <code>ifValue</code> (otherwise this pipe is skipped)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIfValue(String) ifValue}</td><td>see <code>ifParam</code></td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * @author Gerrit van Brakel
 */
public class FixedForwardPipe extends AbstractPipe {

    private String forwardName = "success";
    private PipeForward forward;
	private boolean skipOnEmptyInput=false;
	private String ifParam = null;
	private String ifValue = null;
	
    /**
     * checks for correct configuration of forward
     */
    @Override
    public void configure() throws ConfigurationException {
    	super.configure();
        forward = findForward(forwardName);
        if (forward == null)
            throw new ConfigurationException(getLogPrefix(null) + "has no forward with name [" + forwardName + "]");
    }


    /**
     * called by {@link InputOutputPipeProcessor} to check if the pipe needs to be skipped.
     */
    public PipeRunResult doInitialPipe(Object input, IPipeLineSession session) throws PipeRunException {
		if (isSkipOnEmptyInput() && (input == null || StringUtils.isEmpty(input.toString()))) {
			return new PipeRunResult(getForward(), input);
		}
		if (StringUtils.isNotEmpty(getIfParam())) {
			boolean skipPipe = true;

			ParameterValueList pvl = null;
			if (getParameterList() != null) {
				ParameterResolutionContext prc = new ParameterResolutionContext((String) input, session);
				try {
					pvl = prc.getValues(getParameterList());
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
				return new PipeRunResult(getForward(), input);
			}
		}
		return null;
	}

	protected String getParameterValue(ParameterValueList pvl, String parameterName) {
		ParameterList parameterList = getParameterList();
		if (pvl != null && parameterList != null) {
			for (int i = 0; i < parameterList.size(); i++) {
				Parameter parameter = parameterList.getParameter(i);
				if (parameter.getName().equalsIgnoreCase(parameterName)) {
					return pvl.getParameterValue(i).asStringValue(null);
				}
			}
		}
		return null;
	}

    public PipeForward getForward() {
		return forward;
	}
    
 	/**
 	 * Sets the name of the <code>forward</code> that is looked up upon completion.
 	 */
	@IbisDoc({"1", "Sets the name of the <code>forward</code> that is looked up upon completion.", "success"})
	public void setForwardName(String forwardName) {
        this.forwardName = forwardName;
    }
	public String getForwardName() {
		return forwardName;
	}

	@IbisDoc({"2", "when set, the processing continues directly at the forward of this pipe, without executing the pipe itself", "false"})
	public void setSkipOnEmptyInput(boolean b) {
		skipOnEmptyInput = b;
	}
	public boolean isSkipOnEmptyInput() {
		return skipOnEmptyInput;
	}

	@IbisDoc({"3", "when set, this pipe is only executed when the value of parameter with name <code>ifparam</code> equals <code>ifvalue</code> (otherwise this pipe is skipped)", ""})
	public void setIfParam(String string) {
		ifParam = string;
	}
	public String getIfParam() {
		return ifParam;
	}

	@IbisDoc({"4", "see <code>ifparam</code>", ""})
	public void setIfValue(String string) {
		ifValue = string;
	}
	public String getIfValue() {
		return ifValue;
	}
}
