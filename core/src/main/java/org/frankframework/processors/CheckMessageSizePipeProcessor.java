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
package org.frankframework.processors;

import org.frankframework.core.IExtendedPipe;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.statistics.StatisticsKeeper;
import org.frankframework.stream.Message;
import org.frankframework.util.Misc;

/**
 * @author Jaco de Groot
 */
public class CheckMessageSizePipeProcessor extends PipeProcessorBase {

	@Override
	protected PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		checkMessageSize(message.size(), pipeLine, pipe, true);
		PipeRunResult pipeRunResult = chain.apply(message);

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
