/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.receivers.ResourceLimiter;
import org.frankframework.stream.Message;

/**
 * Processor that limits the number of parallel pipe threads.
 * @author Jaco de Groot
 */
public class LimitingParallelExecutionPipeProcessor extends AbstractPipeProcessor {

	private final Map<IPipe, ResourceLimiter> pipeThreadCounts = new ConcurrentHashMap<>();

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult, PipeRunException> chain) throws PipeRunException {
		ResourceLimiter threadCountLimiter = getThreadLimiter(pipe);
		if (threadCountLimiter == null) { // no restrictions on the maximum number of threads
			return chain.apply(message);
		}
		long waitingDuration;
		try {
			// keep waiting statistics for thread-limited pipes
			long startWaiting = System.currentTimeMillis();
			threadCountLimiter.acquire();
			waitingDuration = System.currentTimeMillis() - startWaiting;
			DistributionSummary summary = pipeLine.getPipeWaitStatistics(pipe);
			summary.record(waitingDuration);
			return chain.apply(message);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new PipeRunException(pipe, "Interrupted acquiring Pipe thread count limiter", e);
		} finally {
			threadCountLimiter.release();
		}
	}

	// method needs to be overridden to enable AOP for debugger
	@Override
	public PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession) throws PipeRunException {
		return super.processPipe(pipeLine, pipe, message, pipeLineSession);
	}

	// method needs to be overridden to enable AOP for debugger
	@Override
	public PipeRunResult validate(@Nonnull PipeLine pipeLine, @Nonnull IValidator validator, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, String messageRoot) throws PipeRunException {
		return super.validate(pipeLine, validator, message, pipeLineSession, messageRoot);
	}

	private ResourceLimiter getThreadLimiter(IPipe pipe) {
		return pipeThreadCounts.computeIfAbsent(pipe, k -> k.getMaxThreads() > 0 ? new ResourceLimiter(k.getMaxThreads()) : null);
	}

}
