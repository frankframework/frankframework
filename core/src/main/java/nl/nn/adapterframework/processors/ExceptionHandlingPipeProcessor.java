/*
   Copyright 2020-2023 WeAreFrank!

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
package nl.nn.adapterframework.processors;

import java.time.Instant;
import java.util.Map;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.pipes.ExceptionPipe;
import nl.nn.adapterframework.stream.Message;

public class ExceptionHandlingPipeProcessor extends PipeProcessorBase {

	@Override
	protected PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		try {
			return chain.apply(message);
		} catch (Exception e) {
			Map<String, PipeForward> forwards = pipe.getForwards();
			if (forwards!=null && forwards.containsKey(PipeForward.EXCEPTION_FORWARD_NAME) && !(pipe instanceof ExceptionPipe)) {

				Instant tsReceivedDate = pipeLineSession.getTsReceived();
				long tsReceivedLong = 0L;
				if(tsReceivedDate != null) {
					tsReceivedLong = tsReceivedDate.toEpochMilli();
				}

				ErrorMessageFormatter emf = new ErrorMessageFormatter();
				Message errorMessage = emf.format(e.getMessage(), e.getCause(), pipeLine.getOwner(), message, pipeLineSession.getMessageId(), tsReceivedLong);
				log.info("exception occured, forwarding to exception-forward [{}], exception:\n", PipeForward.EXCEPTION_FORWARD_NAME, e);
				return new PipeRunResult(pipe.getForwards().get(PipeForward.EXCEPTION_FORWARD_NAME), errorMessage);
			}
			throw e;
		}
	}
}
