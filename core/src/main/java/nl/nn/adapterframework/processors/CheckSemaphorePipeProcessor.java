/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.util.concurrent.ConcurrentHashMap;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Semaphore;

/**
 * @author Jaco de Groot
 */
public class CheckSemaphorePipeProcessor extends PipeProcessorBase {

	private Map<IPipe,Semaphore> pipeThreadCounts=new ConcurrentHashMap<>();

	@Override
	protected PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		PipeRunResult pipeRunResult;
		Semaphore s = getSemaphore(pipe);
		if (s != null) {
			long waitingDuration = 0;
			try {
				// keep waiting statistics for thread-limited pipes
				long startWaiting = System.currentTimeMillis();
				s.acquire();
				waitingDuration = System.currentTimeMillis() - startWaiting;
				StatisticsKeeper sk = pipeLine.getPipeWaitingStatistics(pipe);
				sk.addValue(waitingDuration);
				pipeRunResult = chain.apply(message);
			} catch(InterruptedException e) {
				throw new PipeRunException(pipe, "Interrupted acquiring semaphore", e);
			} finally {
				s.release();
			}
		} else { //no restrictions on the maximum number of threads (s==null)
			pipeRunResult = chain.apply(message);
		}
		return pipeRunResult;
	}

	// method needs to be overridden to enable AOP for debugger
	@Override
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
		return super.processPipe(pipeLine, pipe, message, pipeLineSession);
	}

	// method needs to be overridden to enable AOP for debugger
	@Override
	public PipeRunResult validate(PipeLine pipeLine, IValidator validator, Message message, PipeLineSession pipeLineSession, String messageRoot) throws PipeRunException {
		return super.validate(pipeLine, validator, message, pipeLineSession, messageRoot);
	}

	private Semaphore getSemaphore(IPipe pipe) {
		return pipeThreadCounts.computeIfAbsent(pipe, k -> k.getMaxThreads()>0 ? new Semaphore(k.getMaxThreads()) : null);
	}

}
