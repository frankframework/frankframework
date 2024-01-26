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
import org.apache.logging.log4j.ThreadContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
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
@ElementType(ElementTypes.SESSION)
@Log4j2
public class LogContextPipe extends FixedForwardPipe {

	/**
	 * If set <code>true</code> the ThreadContext parameters will be exported from the current PipeLine up in the call tree.
	 * @ff.default false
	 */
	private @Getter @Setter boolean export;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (!getParameterList().isEmpty()) {
			Map<String,String> values = new LinkedHashMap<>();
			try {
				if (!message.isRepeatable()) {
					message.preserve();
				}
				ParameterValueList pvl = getParameterList().getValues(message, session);
				for(ParameterValue pv : pvl) {
					values.put(pv.getName(), pv.asStringValue());
				}
			} catch (Exception e) {
				log.warn("Exception getting parameter values. Ignoring.", e);
			}
			if (isExport()) {
				ThreadContext.putAll(values);
			} else {
				session.scheduleCloseOnSessionExit(CloseableThreadContext.putAll(values), ClassUtils.nameOf(this));
			}
		}
		return new PipeRunResult(getSuccessForward(),message);
	}

}
