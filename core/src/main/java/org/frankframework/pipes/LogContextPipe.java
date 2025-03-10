/*
   Copyright 2022-2024 WeAreFrank!

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

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.CloseableThreadContext;
import org.apache.logging.log4j.ThreadContext;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

/**
 * Pipe that stores all its parameter values in the ThreadContext, formerly known as Mapped Diagnostic Context (MDC), to be used in logging.
 * The input is passed through to the output.
 *
 * @ff.parameters every parameter value is stored in the ThreadContext under its name.
 *
 * @author Gerrit van Brakel
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.SESSION)
@Log4j2
public class LogContextPipe extends FixedForwardPipe {

	/**
	 * If {@code true} the ThreadContext parameters will be exported from the current PipeLine up in the call tree.
	 * @ff.default false
	 */
	private @Getter @Setter boolean export;

	/**
	 * If {@code true} the pipe will never forward to the {@code ExceptionForward} even if an error occurred during execution.
	 * @ff.default false
	 */
	private @Getter @Setter boolean continueOnError;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		if (!getParameterList().isEmpty()) {
			Map<String,String> values = new LinkedHashMap<>();
			try {
				ParameterValueList pvl = getParameterList().getValues(message, session);
				for (ParameterValue pv : pvl) {
					values.put(pv.getName(), pv.asStringValue());
				}
			} catch (ParameterException e) {
				if (!continueOnError) {
					throw new PipeRunException(this, "exception extracting value for parameter [" + e.getParameterName() + "]", e);
				}
				log.warn("Exception getting parameter values in parameter {}: {}", e.getParameterName(), e.getMessage(), e);
				values.put(e.getParameterName(), e.getMessage());
			} catch (Exception e) {
				if (!continueOnError) {
					throw new PipeRunException(this, "exception extracting parameters", e);
				}
				log.warn("Exception getting parameter values: {}. Ignoring.", e.getMessage(), e);
			}
			if (isExport()) {
				ThreadContext.putAll(values);
			} else {
				session.scheduleCloseOnSessionExit(CloseableThreadContext.putAll(values));
			}
		}
		return new PipeRunResult(getSuccessForward(),message);
	}

}
