/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022 WeAreFrank!

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

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * Stores parameter values in the PipeLineSession.
 *
 * @ff.parameters the result of each parameter defined will be we stored in the PipeLineSession, under the key specified by the parameter name
 *
 * @author  Peter Leeuwenburgh
 */
@Deprecated(forRemoval = true, since = "7.7.0")
@ConfigurationWarning("Please replace with PutInSessionPipe")
public class PutParametersInSession extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		ParameterList parameterList = getParameterList();
		if (!parameterList.isEmpty()) {
			try {
				ParameterValueList pvl = parameterList.getValues(message, session);
				if (pvl != null) {
					for(ParameterValue pv : pvl) {
						String name  = pv.getName();
						Object value = pv.getValue();
						session.put(name, value);
						log.debug("stored [{}] in pipeLineSession under key [{}]", value, name);
					}
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, "exception extracting parameters", e);
			}
		}
		return new PipeRunResult(getSuccessForward(), message);
	}
}
