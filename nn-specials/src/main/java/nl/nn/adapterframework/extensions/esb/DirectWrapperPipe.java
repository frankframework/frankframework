/*
   Copyright 2015, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.pipes.TimeoutGuardPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Kind of extension to EsbSoapWrapperPipe for real time destinations.
 *
 * @author Peter Leeuwenburgh
 */

@Category("NN-Special")
public class DirectWrapperPipe extends TimeoutGuardPipe {
	protected static final String DESTINATION = "destination";
	protected static final String CMHVERSION = "cmhVersion";
	protected static final String ADDOUTPUTNAMESPACE = "addOutputNamespace";

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl = null;
		if (getParameterList() != null) {
			try {
				pvl = getParameterList().getValues(message, session);
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}

		String destination = getParameterValue(pvl, DESTINATION);
		String cmhVersion = getParameterValue(pvl, CMHVERSION);
		String addOutputNamespace = getParameterValue(pvl, ADDOUTPUTNAMESPACE);

		EsbSoapWrapperPipe eswPipe = SpringUtils.createBean(getApplicationContext(), EsbSoapWrapperPipe.class);
		if (addOutputNamespace != null) {
			if ("on".equalsIgnoreCase(addOutputNamespace)) {
				eswPipe.setAddOutputNamespace(true);
			}
		}
		if (destination != null) {
			Parameter destinationParameter = SpringUtils.createBean(getApplicationContext(), Parameter.class);
			destinationParameter.setName(DESTINATION);
			destinationParameter.setValue(destination);
			eswPipe.addParameter(destinationParameter);
		}
		if (cmhVersion != null) {
			if (StringUtils.isNumeric(cmhVersion)) {
				eswPipe.setCmhVersion(Integer.parseInt(cmhVersion));
			}
		}
		try {
			eswPipe.registerForward(getSuccessForward());
			eswPipe.configure();
			return eswPipe.doPipe(message, session);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on wrapping input", e);
		}
	}
}