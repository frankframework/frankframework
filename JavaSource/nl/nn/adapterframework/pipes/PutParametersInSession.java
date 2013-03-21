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
/*
 * $Log: PutParametersInSession.java,v $
 * Revision 1.2  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.1  2012/04/16 12:43:34  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Initial version
 *
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
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the Pipe</td><td>&nbsp;</td></tr>
 * </table>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.parameters.Parameter param}</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author  Peter Leeuwenburgh
 * @version $Id$
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
