/*
   Copyright 2021 WeAreFrank!

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
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Semaphore;

/**
 * @author Gerrit van Brakel
 */
public class CheckSemaphorePipeLineProcessor extends PipeLineProcessorBase {

	private Map<PipeLine,Semaphore> pipeLineThreadCounts=new ConcurrentHashMap<>();

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		PipeLineResult pipeLineResult;
		Semaphore s = getSemaphore(pipeLine);
		if (s != null) {
			long waitingDuration = 0;
			IPipe pipe = pipeLine.getPipe(firstPipe);
			try {
				// keep waiting statistics for thread-limited pipes
				long startWaiting = System.currentTimeMillis();
				s.acquire();
				waitingDuration = System.currentTimeMillis() - startWaiting;
				StatisticsKeeper sk = pipeLine.getPipeWaitingStatistics(pipe);
				sk.addValue(waitingDuration);
				pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
			} catch(InterruptedException e) {
				throw new PipeRunException(pipe, "Interrupted acquiring PipeLine semaphore", e);
			} finally {
				s.release();
			}
		} else { //no restrictions on the maximum number of threads (s==null)
			pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
		return pipeLineResult;
	}

	private Semaphore getSemaphore(PipeLine pipeLine) {
		return pipeLineThreadCounts.computeIfAbsent(pipeLine, k -> k.getMaxThreads()>0 ? new Semaphore(k.getMaxThreads()) : null);
	}

}
