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

@Log4j2
public class MonitoringPipeProcessor extends AbstractPipeProcessor {

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nonnull Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult, PipeRunException> chain) throws PipeRunException {
		long pipeStartTime = System.currentTimeMillis();

		try {
			return chain.apply(message);
		} catch (PipeRunException pre) {
			pipe.throwEvent(IPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			throw pre;
		} catch (RuntimeException re) {
			pipe.throwEvent(IPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			throw new PipeRunException(pipe, "uncaught runtime exception while executing pipe", re);
		} finally {
			long pipeDuration = System.currentTimeMillis() - pipeStartTime;
			DistributionSummary summary = pipeLine.getPipeStatistics(pipe);
			summary.record(pipeDuration);

			if (pipe.getDurationThreshold() >= 0 && pipeDuration > pipe.getDurationThreshold()) {
				log.warn("message [{}] duration [{}] ms exceeds maximum allowed threshold of [{}]", message::getObjectId, () -> pipeDuration, pipe::getDurationThreshold);
				pipe.throwEvent(IPipe.LONG_DURATION_MONITORING_EVENT, message);
			}
		}
	}
}
