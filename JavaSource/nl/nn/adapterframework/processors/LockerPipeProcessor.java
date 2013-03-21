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
 * $Log: LockerPipeProcessor.java,v $
 * Revision 1.1  2012-10-10 10:19:37  m00f069
 * Made it possible to use Locker on Pipe level too
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.Locker;

/**
 * @author Jaco de Groot
 * @version $Id$
 */
public class LockerPipeProcessor extends PipeProcessorBase {

	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, String messageId, Object message, IPipeLineSession pipeLineSession) throws PipeRunException {
		PipeRunResult pipeRunResult;
		IExtendedPipe extendedPipe = null;
		Locker locker = null;
		String objectId = null;
		if (pipe instanceof IExtendedPipe) {
			extendedPipe = (IExtendedPipe)pipe;
			locker = extendedPipe.getLocker();
		}
		if (locker != null) {
			try {
				objectId = locker.lock();
			} catch (Exception e) {
				throw new PipeRunException(pipe, "error while setting lock", e);
			}
		}
		if (objectId != null) {
			try {
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
			} finally {
				try {
					locker.unlock(objectId);
				} catch (Exception e) {
					throw new PipeRunException(pipe, "error while removing lock", e);
				}
			}
		} else {
			pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
		}
		return pipeRunResult;
	}

}
