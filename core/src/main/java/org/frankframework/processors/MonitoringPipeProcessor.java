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
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import io.micrometer.core.instrument.DistributionSummary;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * @author Jaco de Groot
 */
public class MonitoringPipeProcessor extends AbstractPipeProcessor {
	private static final Logger DURATION_LOG = LogUtil.getLogger("LongDurationMessages");

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult, PipeRunException> chain) throws PipeRunException {
		long pipeStartTime = System.currentTimeMillis();
		doDebugLogging(pipeLine, pipe, message, pipeLineSession);

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
				DURATION_LOG.info("Pipe [{}] of [{}] duration [{}] ms exceeds max [{}], message [{}]", pipe::getName, () -> pipeLine.getOwner().getName(), () -> pipeDuration, pipe::getDurationThreshold, () -> message);
				pipe.throwEvent(IPipe.LONG_DURATION_MONITORING_EVENT);
			}
		}
	}

	private void doDebugLogging(final PipeLine pipeLine, final IPipe pipe, Message message, PipeLineSession pipeLineSession) {
		if (!log.isDebugEnabled()) {
			return;
		}
		String ownerName = pipeLine.getOwner() == null ? "<null>" : pipeLine.getOwner().getName();
		StringBuilder sb = new StringBuilder();
		sb.append("Pipeline of adapter [").append(ownerName).append("] messageId [").append(pipeLineSession.getMessageId()).append("] is about to call pipe [").append(pipe.getName()).append("]");

		boolean lir = AppConstants.getInstance().getBoolean("log.logIntermediaryResults", false);
		if (StringUtils.isNotEmpty(pipe.getLogIntermediaryResults())) {
			lir = Boolean.parseBoolean(pipe.getLogIntermediaryResults());
		}
		if (lir) {
			sb.append(" current result ").append(message == null ? "<null>" : "(" + message.getClass().getSimpleName() + ") [" + message + "]").append(" ");
		}
		log.debug(sb.toString());
	}

}
