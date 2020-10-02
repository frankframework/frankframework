/*
   Copyright 2013, 2020 Nationale-Nederlanden

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

import java.util.Map;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.errormessageformatters.ErrorMessageFormatter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;

public class ExceptionHandlingPipeProcessor extends PipeProcessorBase {

	@Override
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession)
			throws PipeRunException {
		PipeRunResult prr = null;
		try {
			prr = pipeProcessor.processPipe(pipeLine, pipe, message, pipeLineSession);
		} catch (PipeRunException e) {
			Map<String, PipeForward> forwards = pipe.getForwards();
			if (forwards!=null && forwards.containsKey("exception")) {
				long tsReceived = DateUtils.parseToDate((String) pipeLineSession.get(IPipeLineSession.tsReceivedKey), DateUtils.FORMAT_FULL_GENERIC).getTime();
				ErrorMessageFormatter emf = new ErrorMessageFormatter();
				String errorMessage = emf.format(e.getMessage(), e.getCause(), pipeLine.getOwner(), message, pipeLineSession.getMessageId(), tsReceived);
				return new PipeRunResult(pipe.getForwards().get("exception"), errorMessage);
			}
			throw e;
		}
		return prr;
	}

}
