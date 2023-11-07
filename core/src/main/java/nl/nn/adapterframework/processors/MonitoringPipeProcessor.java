/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2023 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * @author Jaco de Groot
 */
public class MonitoringPipeProcessor extends PipeProcessorBase {
	private final Logger durationLog = LogUtil.getLogger("LongDurationMessages");

	@Override
	protected PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		PipeRunResult pipeRunResult = null;

		IExtendedPipe pe=null;

		if (pipe instanceof IExtendedPipe) {
			pe = (IExtendedPipe)pipe;
		}

		long pipeStartTime= System.currentTimeMillis();

		if (log.isDebugEnabled()){  // for performance reasons
			StringBuilder sb=new StringBuilder();
			String ownerName=pipeLine.getOwner()==null?"<null>":pipeLine.getOwner().getName();
			String pipeName=pipe==null?"<null>":pipe.getName();
			String messageId = pipeLineSession==null?null:pipeLineSession.getMessageId();
			sb.append("Pipeline of adapter [").append(ownerName).append("] messageId [").append(messageId).append("] is about to call pipe [").append(pipeName).append("]");

			boolean lir = AppConstants.getInstance().getBoolean("log.logIntermediaryResults", false);
			if (pipe instanceof AbstractPipe) {
				AbstractPipe ap = (AbstractPipe) pipe;
				if (StringUtils.isNotEmpty(ap.getLogIntermediaryResults())) {
					lir = Boolean.parseBoolean(ap.getLogIntermediaryResults());
				}
			}
			if (lir) {
				sb.append(" current result ").append(message == null ? "<null>" : "(" + message.getClass().getSimpleName() + ") [" + message + "]").append(" ");
			}

			log.debug(sb.toString());
		}

		// start it
		long pipeDuration = -1;

		try {
			pipeRunResult = chain.apply(message);
		} catch (PipeRunException pre) {
			if (pe!=null) {
				pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			}
			throw pre;
		} catch (RuntimeException re) {
			if (pe!=null) {
				pe.throwEvent(IExtendedPipe.PIPE_EXCEPTION_MONITORING_EVENT);
			}
			throw new PipeRunException(pipe, "Uncaught runtime exception running pipe '" + (pipe==null?"null":pipe.getName()) + "'", re);
		} finally {
			long pipeEndTime = System.currentTimeMillis();
			pipeDuration = pipeEndTime - pipeStartTime;
			StatisticsKeeper sk = pipeLine.getPipeStatistics(pipe);
			if (sk==null) {
				log.warn("Could not get statistics for pipe [+"+pipe.getName()+"]");
			} else {
				sk.addValue(pipeDuration);
			}

			if (pe!=null && pe.getDurationThreshold() >= 0 && pipeDuration > pe.getDurationThreshold()) {
				durationLog.info("Pipe ["+pe.getName()+"] of ["+pipeLine.getOwner().getName()+"] duration ["+pipeDuration+"] ms exceeds max ["+ pe.getDurationThreshold()+ "], message ["+message+"]");
				pe.throwEvent(IExtendedPipe.LONG_DURATION_MONITORING_EVENT);
			}

		}

		return pipeRunResult;
	}

}
