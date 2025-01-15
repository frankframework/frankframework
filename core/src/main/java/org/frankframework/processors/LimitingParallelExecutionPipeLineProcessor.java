/*
   Copyright 2021-2024 WeAreFrank!

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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.micrometer.core.instrument.DistributionSummary;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.receivers.ResourceLimiter;
import org.frankframework.stream.Message;

/**
 * Processor that limits the number of parallel pipeline threads.
 * @author Gerrit van Brakel
 */
public class LimitingParallelExecutionPipeLineProcessor extends AbstractPipeLineProcessor {

	private final Map<PipeLine, ResourceLimiter> pipeLineThreadCounts = new ConcurrentHashMap<>();

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		ResourceLimiter threadCountLimiter = getThreadLimiter(pipeLine);
		if (threadCountLimiter == null) { // no restrictions on the maximum number of threads
			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
		IPipe pipe = pipeLine.getPipe(firstPipe);
		try {
			// keep waiting statistics for thread-limited pipes
			long startWaiting = System.currentTimeMillis();
			threadCountLimiter.acquire();
			long waitingDuration = System.currentTimeMillis() - startWaiting;
			DistributionSummary summary = pipeLine.getPipelineWaitStatistics();
			summary.record(waitingDuration);

			return pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		} catch(InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PipeRunException(pipe, "Interrupted acquiring PipeLine thread count limiter", e);
		} finally {
			threadCountLimiter.release();
		}
	}

	private ResourceLimiter getThreadLimiter(PipeLine pipeLine) {
		return pipeLineThreadCounts.computeIfAbsent(pipeLine, k -> k.getMaxThreads()>0 ? new ResourceLimiter(k.getMaxThreads()) : null);
	}

}
