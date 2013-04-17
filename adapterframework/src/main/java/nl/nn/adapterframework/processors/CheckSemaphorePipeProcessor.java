/*
   Copyright 2013 Nationale-Nederlanden

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

import java.util.Hashtable;
import java.util.Map;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.Semaphore;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class CheckSemaphorePipeProcessor extends PipeProcessorBase {

	private Map pipeThreadCounts=new Hashtable();

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, IPipeLineSession pipeLineSession
			) throws PipeRunException {
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
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
			} catch(InterruptedException e) {
				throw new PipeRunException(pipe, "Interrupted acquiring semaphore", e);
			} finally { 
				s.release();
			}
		} else { //no restrictions on the maximum number of threads (s==null)
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		}
		return pipeRunResult;
	}

	private Semaphore getSemaphore(IPipe pipe) {
		int maxThreads = pipe.getMaxThreads();
		if (maxThreads > 0) {
			Semaphore s;
			synchronized (pipeThreadCounts) {
				if (pipeThreadCounts.containsKey(pipe)) {
					s = (Semaphore) pipeThreadCounts.get(pipe);
				} else {
					s = new Semaphore(maxThreads);
					pipeThreadCounts.put(pipe, s);
				}
			}
			return s;
		}
		return null;
	}

}
