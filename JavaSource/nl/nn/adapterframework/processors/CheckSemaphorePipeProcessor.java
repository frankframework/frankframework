/*
 * $Log: CheckSemaphorePipeProcessor.java,v $
 * Revision 1.4  2011-08-18 14:40:27  L190409
 * use modified interface for statistics
 *
 * Revision 1.3  2010/09/13 13:53:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now extends baseclass
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import java.util.Hashtable;
import java.util.Map;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.Semaphore;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class CheckSemaphorePipeProcessor extends PipeProcessorBase {

	private Map pipeThreadCounts=new Hashtable();

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, PipeLineSession pipeLineSession
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
