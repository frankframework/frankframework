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
/*
 * $Log: TracingEventsPipeProcessor.java,v $
 * Revision 1.7  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.6  2011/11/30 13:51:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2011/08/18 14:40:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use modified interface for statistics
 *
 * Revision 1.3  2010/09/13 13:57:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now extends baseclass
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TracingUtil;

import org.apache.log4j.Logger;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class TracingEventsPipeProcessor extends PipeProcessorBase {
	private Logger durationLog = LogUtil.getLogger("LongDurationMessages");

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, String messageId, Object message, IPipeLineSession pipeLineSession) throws PipeRunException {
		PipeRunResult pipeRunResult = null;

		IExtendedPipe pe=null;
			
		if (pipe instanceof IExtendedPipe) {
			pe = (IExtendedPipe)pipe;
		}
    	
		TracingUtil.beforeEvent(pipe);
		long pipeStartTime= System.currentTimeMillis();
		
		if (log.isDebugEnabled()){  // for performance reasons
			StringBuffer sb=new StringBuffer();
			String ownerName=pipeLine.getOwner()==null?"<null>":pipeLine.getOwner().getName();
			String pipeName=pipe==null?"<null>":pipe.getName();
			sb.append("Pipeline of adapter ["+ownerName+"] messageId ["+messageId+"] is about to call pipe ["+ pipeName+"]");

			if (AppConstants.getInstance().getProperty("log.logIntermediaryResults")!=null) {
				if (AppConstants.getInstance().getProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
					sb.append(" current result ["+ message +"] ");
				}
			}
			log.info(sb.toString());
		}

		// start it
		long pipeDuration = -1;
			
		try {
			pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		} catch (PipeRunException pre) {
			TracingUtil.exceptionEvent(pipe);
			if (pe!=null) {
				pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			}
			throw pre;
		} catch (RuntimeException re) {
			TracingUtil.exceptionEvent(pipe);
			if (pe!=null) {
				pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			}
			throw new PipeRunException(pipe, "Uncaught runtime exception running pipe '"
					+ (pipe==null?"null":pipe.getName()) + "'", re);
		} finally {
			TracingUtil.afterEvent(pipe);
			
			long pipeEndTime = System.currentTimeMillis();
			pipeDuration = pipeEndTime - pipeStartTime;
			StatisticsKeeper sk = pipeLine.getPipeStatistics(pipe);
			sk.addValue(pipeDuration);

			if (pe!=null) {
				if (pe.getDurationThreshold() >= 0 && pipeDuration > pe.getDurationThreshold()) {
					durationLog.info("Pipe ["+pe.getName()+"] of ["+pipeLine.getOwner().getName()+"] duration ["+pipeDuration+"] ms exceeds max ["+ pe.getDurationThreshold()+ "], message ["+message+"]");
					pe.throwEvent(IExtendedPipe.LONG_DURATION_MONITORING_EVENT);
				}
			}

		}

		return pipeRunResult;
	}

}
