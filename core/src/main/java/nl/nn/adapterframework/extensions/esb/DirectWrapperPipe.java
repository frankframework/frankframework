/*
   Copyright 2015, 2020 Nationale-Nederlanden

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

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.stream.Message;

/**
 * Kind of extension to EsbSoapWrapperPipe for real time destinations.
 *
 * @author Peter Leeuwenburgh
 */

public class DirectWrapperPipe extends TimeoutGuardPipe {
	protected final static String DESTINATION = "destination";
	protected final static String CMHVERSION = "cmhVersion";
	protected final static String ADDOUTPUTNAMESPACE = "addOutputNamespace";

	@Override
	public Message doPipeWithTimeoutGuarded(Message message, IPipeLineSession session) throws PipeRunException {
		Message result;

		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session) + "exception extracting parameters", e);
			}
		}

		String destination = getParameterValue(pvl, DESTINATION);
		String cmhVersion = getParameterValue(pvl, CMHVERSION);
		String addOutputNamespace = getParameterValue(pvl, ADDOUTPUTNAMESPACE);

		EsbSoapWrapperPipe eswPipe = new EsbSoapWrapperPipe();
		if (addOutputNamespace != null) {
			if ("on".equalsIgnoreCase(addOutputNamespace)) {
				eswPipe.setAddOutputNamespace(true);
			}
		}
		if (destination != null) {
			Parameter p = new Parameter();
			p.setName(DESTINATION);
			p.setValue(destination);
			eswPipe.addParameter(p);
		}
		if (cmhVersion != null) {
			if (StringUtils.isNumeric(cmhVersion)) {
				eswPipe.setCmhVersion(Integer.parseInt(cmhVersion));
			}
		}
		PipeForward pf = new PipeForward();
		pf.setName("success");
		eswPipe.registerForward(pf);
		try {
			eswPipe.configure();
			PipeRunResult prr = eswPipe.doPipe(message, session);
			result = prr.getResult();
		} catch (Exception e) {
			throw new PipeRunException(this, getLogPrefix(session) + "Exception on wrapping input", e);
		}
		return result;
	}
}