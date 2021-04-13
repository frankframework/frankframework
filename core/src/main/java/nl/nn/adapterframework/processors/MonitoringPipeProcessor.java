/*
   Copyright 2013, 2020 Nationale-Nederlanden

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

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * @author Jaco de Groot
 */
public class MonitoringPipeProcessor extends PipeProcessorBase {
	private Logger durationLog = LogUtil.getLogger("LongDurationMessages");

	@Override
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
		PipeRunResult pipeRunResult = null;

		IExtendedPipe pe=null;
			
		if (pipe instanceof IExtendedPipe) {
			pe = (IExtendedPipe)pipe;
		}
    	
		long pipeStartTime= System.currentTimeMillis();
		
		if (log.isDebugEnabled()){  // for performance reasons
			StringBuffer sb=new StringBuffer();
			String ownerName=pipeLine.getOwner()==null?"<null>":pipeLine.getOwner().getName();
			String pipeName=pipe==null?"<null>":pipe.getName();
			String messageId = pipeLineSession==null?null:pipeLineSession.getMessageId();
			sb.append("Pipeline of adapter ["+ownerName+"] messageId ["+messageId+"] is about to call pipe ["+ pipeName+"]");

			boolean lir = false;
			if (AppConstants.getInstance().getProperty("log.logIntermediaryResults")!=null) {
				if (AppConstants.getInstance().getProperty("log.logIntermediaryResults").equalsIgnoreCase("true")) {
					lir = true;
				}
			}
			if (pipe instanceof AbstractPipe) {
				AbstractPipe ap = (AbstractPipe) pipe;
				if (StringUtils.isNotEmpty(ap.getLogIntermediaryResults())) {
					lir = Boolean.valueOf(ap.getLogIntermediaryResults());
				}
			}
			if (lir) {
				sb.append(" current result "+ (message==null?"<null>":"("+message.getClass().getSimpleName()+") ["+message +"]" )+" ");
			}

			log.debug(sb.toString());
		}

		// start it
		long pipeDuration = -1;
			
		try {
			pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, message, pipeLineSession);
		} catch (PipeRunException pre) {
			if (pe!=null) {
				pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			}
			throw pre;
		} catch (RuntimeException re) {
			if (pe!=null) {
				pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			}
			throw new PipeRunException(pipe, "Uncaught runtime exception running pipe '"
					+ (pipe==null?"null":pipe.getName()) + "'", re);
		} finally {
			long pipeEndTime = System.currentTimeMillis();
			pipeDuration = pipeEndTime - pipeStartTime;
			StatisticsKeeper sk = pipeLine.getPipeStatistics(pipe);
			if (sk==null) {
				log.warn("Could not get statistics for pipe [+"+pipe.getName()+"]");
			} else {
				sk.addValue(pipeDuration);
			}

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
