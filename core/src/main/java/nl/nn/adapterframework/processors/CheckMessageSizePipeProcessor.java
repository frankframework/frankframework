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
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.AbstractPipe;
import nl.nn.adapterframework.statistics.StatisticsKeeper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Misc;

/**
 * @author Jaco de Groot
 */
public class CheckMessageSizePipeProcessor extends PipeProcessorBase {

	@Override
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession) throws PipeRunException {
		checkMessageSize(message.size(), pipeLine, pipe, true);
		PipeRunResult pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, message, pipeLineSession);

		Message result = pipeRunResult.getResult();
		checkMessageSize(result.size(), pipeLine, pipe, false);
		return pipeRunResult;
	}

	private void checkMessageSize(long messageLength, PipeLine pipeLine, IPipe pipe, boolean input) {
		if(messageLength > -1) {
			if (pipe instanceof AbstractPipe) {
				AbstractPipe aPipe = (AbstractPipe) pipe;
				StatisticsKeeper sizeStat = null;
				if (input) {
					if (aPipe.getInSizeStatDummyObject() != null) {
						sizeStat = pipeLine.getPipeSizeStatistics(aPipe.getInSizeStatDummyObject());
					}
				} else {
					if (aPipe.getOutSizeStatDummyObject() != null) {
						sizeStat = pipeLine.getPipeSizeStatistics(aPipe.getOutSizeStatDummyObject());
					}
				}
				if (sizeStat!=null) {
					sizeStat.addValue(messageLength);
				}
			}

			if (pipeLine.getMessageSizeWarnNum() >= 0 && messageLength >= pipeLine.getMessageSizeWarnNum()) {
				log.warn(String.format("pipe [%s] of adapter [%s], " + (input ? "input" : "result") + " message size [%s] exceeds [%s]", pipe.getName(), pipeLine.getOwner().getName(), Misc.toFileSize(messageLength), Misc.toFileSize(pipeLine.getMessageSizeWarnNum())));
				if (pipe instanceof IExtendedPipe) {
					IExtendedPipe pe = (IExtendedPipe)pipe;
					pe.throwEvent(IExtendedPipe.MESSAGE_SIZE_MONITORING_EVENT);
				}
			}
		}
	}
}
