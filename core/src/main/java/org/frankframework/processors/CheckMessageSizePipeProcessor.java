/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import io.micrometer.core.instrument.DistributionSummary;
import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.stream.Message;
import org.frankframework.util.Misc;

@Log4j2
public class CheckMessageSizePipeProcessor extends AbstractPipeProcessor {

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nonnull Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		checkMessageSize(message.size(), pipeLine, pipe, true);
		PipeRunResult pipeRunResult = chain.apply(message);

		Message result = pipeRunResult.getResult();
		checkMessageSize(result.size(), pipeLine, pipe, false);
		return pipeRunResult;
	}

	private void checkMessageSize(long messageLength, PipeLine pipeLine, IPipe pipe, boolean input) {
		if(messageLength > -1) {
			if (pipe.sizeStatisticsEnabled()) {
				DistributionSummary sizeStat = null;
				if (input) {
					sizeStat = pipeLine.getPipeSizeInStatistics(pipe);
				} else {
					sizeStat = pipeLine.getPipeSizeOutStatistics(pipe);
				}
				sizeStat.record(messageLength);
			}

			if (pipeLine.getMessageSizeWarnNum() >= 0 && messageLength >= pipeLine.getMessageSizeWarnNum()) {
				log.warn("{} message size [{}] exceeds [{}]", ()-> (input ? "input" : "result"), () -> Misc.toFileSize(messageLength), () -> Misc.toFileSize(pipeLine.getMessageSizeWarnNum()));
				pipe.throwEvent(IPipe.MESSAGE_SIZE_MONITORING_EVENT);
			}
		}
	}

}
