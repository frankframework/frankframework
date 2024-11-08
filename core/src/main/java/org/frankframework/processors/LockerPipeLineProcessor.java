/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.stream.Message;
import org.frankframework.util.Locker;

/**
 * @author Jaco de Groot
 */
public class LockerPipeLineProcessor extends AbstractPipeLineProcessor {

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		PipeLineResult pipeLineResult;
		Locker locker = pipeLine.getLocker();
		String objectId = null;
		if (locker != null) {
			try {
				objectId = locker.acquire();
			} catch (Exception e) {
				throw new PipeRunException(null, "error while setting lock ["+locker+"]", e);
			}
			if (objectId == null) {
				log.info("could not obtain lock [{}]", locker);
				pipeLineResult = new PipeLineResult();
				pipeLineResult.setState(ExitState.SUCCESS);
			} else {
				try {
					pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
				} finally {
					try {
						locker.release(objectId);
					} catch (Exception e) {
						//throw new PipeRunException(null, "error while removing lock", e);
						String msg = "error while removing lock ["+locker+"]: " + e.getMessage();
						log.warn(msg);
					}
				}
			}
		} else {
			pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
		return pipeLineResult;
	}
}
