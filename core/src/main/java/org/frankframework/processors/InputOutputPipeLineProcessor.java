/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2025 WeAreFrank!

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

import org.apache.logging.log4j.CloseableThreadContext;

import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;
import org.frankframework.util.UUIDUtil;

/**
 * @author Jaco de Groot
 */
public class InputOutputPipeLineProcessor extends AbstractPipeLineProcessor {

	// We have tests to validate that the messageId, if already set in the LogContext, is technically overwritten, but not removed upon the try-with-resources closure.
	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		// Reset the PipeLineSession and store the message and its id in the session
		if (messageId == null) {
			messageId = UUIDUtil.createSimpleUUID();
			log.error("messageId not set, creating synthetic id [{}]", messageId);
		}

		if (message == null) {
			throw new PipeRunException(null, "Pipeline of adapter ["+ pipeLine.getOwner().getName()+"] received null message");
		}

		// Store message and messageId in the pipeLineSession
		pipeLineSession.put(PipeLineSession.ORIGINAL_MESSAGE_KEY, message);
		pipeLineSession.put(PipeLineSession.MESSAGE_ID_KEY, messageId);

		// Store messageId in the LogContext, it may already be present but we want to make sure it is set for log traceability.
		try (final CloseableThreadContext.Instance ctc = CloseableThreadContext.put(LogUtil.MDC_MESSAGE_ID_KEY, messageId)) {
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
	}

}
