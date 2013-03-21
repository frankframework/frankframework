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
 * $Log: LockerPipeLineProcessor.java,v $
 * Revision 1.1  2012-11-22 13:41:05  m00f069
 * Made it possible to use Locker on PipeLine level too
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.Locker;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class LockerPipeLineProcessor extends PipeLineProcessorBase {

	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, String message, IPipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		PipeLineResult pipeLineResult;
		Locker locker = pipeLine.getLocker();
		String objectId = null;
		if (locker != null) {
			try {
				objectId = locker.lock();
			} catch (Exception e) {
				throw new PipeRunException(null, "error while setting lock", e);
			}
		}
		if (objectId != null) {
			try {
				pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
			} finally {
				try {
					locker.unlock(objectId);
				} catch (Exception e) {
					throw new PipeRunException(null, "error while removing lock", e);
				}
			}
		} else {
			pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);
		}
		return pipeLineResult;
	}

}
