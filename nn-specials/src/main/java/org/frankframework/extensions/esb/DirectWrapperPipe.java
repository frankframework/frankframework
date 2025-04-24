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
package org.frankframework.extensions.esb;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.Category;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.pipes.TimeoutGuardPipe;
import org.frankframework.stream.Message;
import org.frankframework.util.SpringUtils;

/**
 * Kind of extension to EsbSoapWrapperPipe for real time destinations.
 *
 * @author Peter Leeuwenburgh
 */
@Category(Category.Type.NN_SPECIAL)
public class DirectWrapperPipe extends TimeoutGuardPipe {
	protected static final String DESTINATION = "destination";
	protected static final String CMHVERSION = "cmhVersion";
	protected static final String ADDOUTPUTNAMESPACE = "addOutputNamespace";

	@Override
	public PipeRunResult doPipeWithTimeoutGuarded(Message message, PipeLineSession session) throws PipeRunException {
		ParameterValueList pvl;
		try {
			pvl = getParameterList().getValues(message, session);
		} catch (ParameterException e) {
			throw new PipeRunException(this, "exception extracting parameters", e);
		}

		String destination = getParameterValue(pvl, DESTINATION);
		String cmhVersion = getParameterValue(pvl, CMHVERSION);
		String addOutputNamespace = getParameterValue(pvl, ADDOUTPUTNAMESPACE);

		EsbSoapWrapperPipe eswPipe = SpringUtils.createBean(getApplicationContext());
		if (addOutputNamespace != null) {
			if ("on".equalsIgnoreCase(addOutputNamespace)) {
				eswPipe.setAddOutputNamespace(true);
			}
		}
		if (destination != null) {
			Parameter destinationParameter = SpringUtils.createBean(getApplicationContext());
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
			eswPipe.addForward(getSuccessForward());
			eswPipe.configure();
			return eswPipe.doPipe(message, session);
		} catch (Exception e) {
			throw new PipeRunException(this, "Exception on wrapping input", e);
		}
	}
}
