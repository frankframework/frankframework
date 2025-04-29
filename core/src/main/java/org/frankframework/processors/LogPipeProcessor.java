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
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;

@Log4j2
public class LogPipeProcessor extends AbstractPipeProcessor {

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult, PipeRunException> chain) throws PipeRunException {
		if (!log.isDebugEnabled()) {
			doDebugLogging(pipeLine, pipe, message, pipeLineSession);
		}

		return chain.apply(message);
	}

	private void doDebugLogging(final PipeLine pipeLine, final IPipe pipe, Message message, PipeLineSession pipeLineSession) {
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
