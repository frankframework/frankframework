/*
   Copyright 2022 WeAreFrank!

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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.CloseableThreadContext;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * Pipe that stores all its parameter values in the ThreadContext, formerly known as Mapped Diagnostic Context (MDC), to be used in logging.
 * The input is passed through to the output.
 *
 * @ff.parameters every parameter value is stored in the ThreadContext under its name.
 *
 * @author Gerrit van Brakel
 */
public class MdcPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (!getParameterList().isEmpty()) {
			Map<String,String> values = new LinkedHashMap<>();
			try {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				for(ParameterValue pv : pvl) {
					values.put(pv.getName(), pv.asStringValue());
				}
			} catch (ParameterException e) {
				throw new PipeRunException(this, getLogPrefix(session)+"exception extracting parameters", e);
			}
			session.scheduleCloseOnSessionExit(CloseableThreadContext.putAll(values), ClassUtils.nameOf(this));
		}
		return new PipeRunResult(getSuccessForward(),message);
	}

}
