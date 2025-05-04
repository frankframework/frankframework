/*
   Copyright 2025 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.CloseableThreadContext;

import lombok.extern.log4j.Log4j2;

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
 * Logs the pipe name and the message before calling the pipe.
 * <p>
 * Sets the LogContext with the pipe name, so it can be used in all subsequent log messages.
 * <p>
 * Also used to log the intermediary results between pipes, when enabled (either on pipe or global level).
 */
@Log4j2
public class LogPipeProcessor extends AbstractPipeProcessor {
	private static final boolean LOG_INTERMEDIARY_RESULTS = AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true);

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nonnull Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult, PipeRunException> chain) throws PipeRunException {
		if (log.isDebugEnabled() && logIntermediaryResults(pipe)) {
			log.debug("pipeline process is about to call pipe [{}] current result [{}]", pipe::getName, message::toString);
		} else {
			log.info("pipeline process is about to call pipe [{}]", pipe::getName);
		}


		try (CloseableThreadContext.Instance ignored = CloseableThreadContext.put(LogUtil.MDC_PIPE_KEY, pipe.getName())) {
			return chain.apply(message);
		}
	}


	/**
	 * Indicates whether the results between calling pipes have to be logged.
	 * Only used if the log level is set to DEBUG.
	 */
	private boolean logIntermediaryResults(IPipe pipe) {
		if (StringUtils.isNotEmpty(pipe.getLogIntermediaryResults())) {
			return Boolean.parseBoolean(pipe.getLogIntermediaryResults());
		}
		return LOG_INTERMEDIARY_RESULTS;
	}
}
