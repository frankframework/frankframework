/*
   Copyright 2024 WeAreFrank!

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

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.stream.Message;

/**
 * Adds the current pipe to the message context / metadata to be able to use it as a param. This should be configured as the wrapping bean for
 * CorePipeProcessor.
 *
 * @author evandongen
 */
public class TrackPreviousPipeInMetadataProcessor extends AbstractPipeProcessor {

	public static final String CONTEXT_PREVIOUS_PIPE = "Pipeline.PreviousPipe";

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession,
										@Nonnull ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		PipeRunResult pipeRunResult = chain.apply(message);

		if (logPreviousPipe(pipe, message, pipeLineSession)) {
			message.getContext().put(CONTEXT_PREVIOUS_PIPE, pipe.getName());
		}

		return pipeRunResult;
	}

	private boolean logPreviousPipe(IPipe pipe, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
		// FixedForwardPipe pipes can be skipped, if it's skipped, we don't need to update the 'previous pipe' value
		if (pipe instanceof FixedForwardPipe ffPipe) {
			return !ffPipe.skipPipe(message, pipeLineSession);
		} else {
			return true;
		}
	}
}
