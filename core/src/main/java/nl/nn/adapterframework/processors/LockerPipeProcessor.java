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
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IExtendedPipe;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.Locker;

/**
 * @author Jaco de Groot
 */
public class LockerPipeProcessor extends PipeProcessorBase {

	@Override
	protected PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
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
				objectId = locker.acquire();
			} catch (Exception e) {
				throw new PipeRunException(pipe, "error while trying to obtain lock ["+locker+"]", e);
			}
			if (objectId == null) {
				throw new PipeRunException(pipe, "could not obtain lock ["+locker+"]");
			}
			try {
				pipeRunResult = chain.apply(message);
			} finally {
				try {
					locker.release(objectId);
				} catch (Exception e) {
					throw new PipeRunException(pipe, "error while removing lock", e);
				}
			}
		} else {
			pipeRunResult = chain.apply(message);
		}
		return pipeRunResult;
	}

}
