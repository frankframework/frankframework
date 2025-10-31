/*
   Copyright 2020-2025 WeAreFrank!

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
package org.frankframework.processors;

import jakarta.annotation.Nonnull;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.HasName;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.pipes.ExceptionPipe;
import org.frankframework.stream.Message;

@Log4j2
public class ExceptionHandlingPipeProcessor extends AbstractPipeProcessor {

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nonnull Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		try {
			return chain.apply(message);
		} catch (Exception e) {
			PipeForward exceptionForward = pipe.findForward(PipeForward.EXCEPTION_FORWARD_NAME);
			if (exceptionForward != null && !(pipe instanceof ExceptionPipe)) {
				final Message errorMessage;
				if(e instanceof PipeRunException exception) {
					HasName location = exception.getPipeInError();
					Throwable exceptionToPass = exception.getParameters().isEmpty() ? exception.getCause() : exception;
					errorMessage = pipeLine.getAdapter().formatErrorMessage(null, exceptionToPass, message, pipeLineSession, location);
				} else {
					errorMessage = pipeLine.getAdapter().formatErrorMessage(null, e, message, pipeLineSession, pipeLine.getAdapter());
				}

				log.info("exception occurred, forwarding to exception-forward [{}]", PipeForward.EXCEPTION_FORWARD_NAME, e);
				return new PipeRunResult(exceptionForward, errorMessage);
			}
			throw e;
		}
	}
}
