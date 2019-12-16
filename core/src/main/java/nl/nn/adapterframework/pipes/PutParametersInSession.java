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

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValueList;

/**
 * Puts each parameter value in the PipeLineSession, under the key specified by the parameter name.
 *
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link Parameter param}</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 */
public class PutParametersInSession extends FixedForwardPipe {
	
	public PipeRunResult doPipe(Object input, IPipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		ParameterList parameterList = getParameterList();
		if (parameterList != null) {
			ParameterResolutionContext prc = new ParameterResolutionContext((String) input, session);
			try {
				pvl = prc.getValues(getParameterList());
				if (pvl != null) {
					for (int i = 0; i < parameterList.size(); i++) {
						Parameter parameter = parameterList.getParameter(i);
						String pn = parameter.getName();
						Object pv = parameter.getValue(pvl, prc);
						session.put(pn, pv);
						log.debug(getLogPrefix(session)+"stored ["+pv+"] in pipeLineSession under key ["+pn+"]");
					}
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}
		return new PipeRunResult(getForward(), input);
	}
}
