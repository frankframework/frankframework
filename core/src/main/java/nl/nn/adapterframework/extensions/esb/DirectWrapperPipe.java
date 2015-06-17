/*
   Copyright 2015 Nationale-Nederlanden

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
package nl.nn.adapterframework.extensions.esb;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;

/**
 * Kind of extension to EsbSoapWrapperPipe for real time destinations.
 *
 * @author Peter Leeuwenburgh
 */

public class DirectWrapperPipe extends TimeoutGuardPipe {
	protected final static String DESTINATION = "destination";

	public String doPipeWithTimeoutGuarded(Object input,
			IPipeLineSession session) throws PipeRunException {
		String result;

		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext(
					(String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session)
						+ "exception extracting parameters", e);
			}
		}

		String destination = getParameterValue(pvl, DESTINATION);

		EsbSoapWrapperPipe eswPipe = new EsbSoapWrapperPipe();
		eswPipe.setAddOutputNamespace(true);
		if (destination != null) {
			Parameter p = new Parameter();
			p.setName(DESTINATION);
			p.setValue(destination);
			eswPipe.addParameter(p);
		}
		PipeForward pf = new PipeForward();
		pf.setName("success");
		eswPipe.registerForward(pf);
		try {
			eswPipe.configure();
			PipeRunResult prr = eswPipe.doPipe(input, session);
			result = (String) prr.getResult();
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session)
					+ "Exception on wrapping input", e);
		}
		return result;
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
}