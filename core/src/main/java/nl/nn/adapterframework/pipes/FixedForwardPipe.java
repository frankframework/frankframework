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
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

/**
 * Provides a base-class for a Pipe that always has the same forward.
 * Ancestor classes should call <code>super.configure()</code> in their <code>configure()</code>-methods.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.pipes.FixedForwardPipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMaxThreads(int) maxThreads}</td><td>maximum number of threads that may call {@link #doPipe(java.lang.Object, nl.nn.adapterframework.core.IPipeLineSession)} simultaneously</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setDurationThreshold(long) durationThreshold}</td><td>if durationThreshold >=0 and the duration (in milliseconds) of the message processing exceeded the value specified, then the message is logged informatory</td><td>-1</td></tr>
 * <tr><td>{@link #setGetInputFromSessionKey(String) getInputFromSessionKey}</td><td>when set, input is taken from this session key, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setGetInputFromFixedValue(String) getInputFromFixedValue}</td><td>when set, this fixed value is taken as input, instead of regular input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setEmptyInputReplacement(String) emptyInputReplacement}</td><td>when set and the regular input is empty, this fixed value is taken as input</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreResultInSessionKey(String) storeResultInSessionKey}</td><td>when set, the result is stored under this session key</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPreserveInput(boolean) preserveInput}</td><td>when set <code>true</code>, the input of a pipe is restored before processing the next one</td><td>false</td></tr>
 * <tr><td>{@link #setNamespaceAware(boolean) namespaceAware}</td><td>controls namespace-awareness of possible XML parsing in descender-classes</td><td>application default</td></tr>
 * <tr><td>{@link #setTransactionAttribute(String) transactionAttribute}</td><td>Defines transaction and isolation behaviour. Equal to <A href="http://java.sun.com/j2ee/sdk_1.2.1/techdocs/guides/ejb/html/Transaction2.html#10494">EJB transaction attribute</a>. Possible values are:
 *   <table border="1">
 *   <tr><th>transactionAttribute</th><th>callers Transaction</th><th>Pipe excecuted in Transaction</th></tr>
 *   <tr><td colspan="1" rowspan="2">Required</td>    <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">RequiresNew</td> <td>none</td><td>T2</td></tr>
 * 											      <tr><td>T1</td>  <td>T2</td></tr>
 *   <tr><td colspan="1" rowspan="2">Mandatory</td>   <td>none</td><td>error</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">NotSupported</td><td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>none</td></tr>
 *   <tr><td colspan="1" rowspan="2">Supports</td>    <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>T1</td></tr>
 *   <tr><td colspan="1" rowspan="2">Never</td>       <td>none</td><td>none</td></tr>
 * 											      <tr><td>T1</td>  <td>error</td></tr>
 *  </table></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td>  <td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link #setSkipOnEmptyInput(String) skipOnEmptyInput}</td><td>when set, this pipe is skipped</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIfParam(String) ifParam}</td><td>when set, this pipe is only executed when the value of parameter with name <code>ifParam</code> equals <code>ifValue</code> (otherwise this pipe is skipped)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setIfValue(String) ifValue}</td><td>see <code>ifParam</code></td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
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

	public PipeRunResult doInitialPipe(Object input, IPipeLineSession session)
			throws PipeRunException {
		if ((input == null || StringUtils.isEmpty(input.toString()))
				&& isSkipOnEmptyInput()) {
			return new PipeRunResult(getForward(), input);
		}
		if (getIfParam() != null) {
			boolean skipPipe = true;

			ParameterValueList pvl = null;
			if (getParameterList() != null) {
				ParameterResolutionContext prc = new ParameterResolutionContext(
						(String) input, session);
				try {
					pvl = prc.getValues(getParameterList());
				} catch (ParameterException e) {
					throw new PipeRunException(this, getLogPrefix(session)
							+ "exception on extracting parameters", e);
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

	private String getParameterValue(ParameterValueList pvl,
			String parameterName) {
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

    protected PipeForward getForward() {
		return forward;
	}
 	/**
 	 * Sets the name of the <code>forward</code> that is looked up
 	 * upon completion.
 	 */
	public void setForwardName(String forwardName) {
        this.forwardName = forwardName;
    }
	public String getForwardName() {
		return forwardName;
	}

	public void setSkipOnEmptyInput(boolean b) {
		skipOnEmptyInput = b;
	}

	public boolean isSkipOnEmptyInput() {
		return skipOnEmptyInput;
	}

	public void setIfParam(String string) {
		ifParam = string;
	}

	public String getIfParam() {
		return ifParam;
	}

	public void setIfValue(String string) {
		ifValue = string;
	}

	public String getIfValue() {
		return ifValue;
	}
}
