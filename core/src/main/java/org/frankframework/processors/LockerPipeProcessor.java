/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021-2026 WeAreFrank!

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

import org.jspecify.annotations.NonNull;

import org.frankframework.core.IPipe;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.stream.Message;
import org.frankframework.util.Locker;

/**
 * @author Jaco de Groot
 */
public class LockerPipeProcessor extends AbstractPipeProcessor {

	@NonNull
	@Override
	@SuppressWarnings({ "java:S1193", "java:S1181", "java:S1143", "java:S1163", "ThrowFromFinallyBlock" }) // java:S1193: instanceof in catch block. Prevents us from duplicating the throw PipeRunException. java:S1143, java:S1163: We want to throw from finally, sorry. java:S1181: Catching Throwable. Because we want to add the Throwable to suppressedExceptions.
	protected PipeRunResult processPipe(@NonNull PipeLine pipeLine, @NonNull IPipe pipe, @NonNull Message message, @NonNull PipeLineSession pipeLineSession, @NonNull ThrowingFunction<Message, PipeRunResult, PipeRunException> chain) throws PipeRunException {
		String objectId;
		Locker locker = pipe.getLocker();
		if (locker == null) {
			return chain.apply(message);
		}

		try {
			objectId = locker.acquire();
		} catch (Exception e) {
			if (e instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}
			throw new PipeRunException(pipe, "error while trying to obtain lock [" + locker + "]", e);
		}
		if (objectId == null) {
			throw new PipeRunException(pipe, "could not obtain lock [" + locker + "]");
		}
		Throwable tThrown = null;
		try {
			return chain.apply(message);
		} catch (Throwable e) {
			tThrown = e;
			throw e;
		} finally {
			try {
				locker.release(objectId);
			} catch (Exception e) {
				PipeRunException pre = new PipeRunException(pipe, "error while removing lock", e);
				if (tThrown != null) {
					pre.addSuppressed(tThrown);
				}
				throw pre;
			}
		}
	}

}
