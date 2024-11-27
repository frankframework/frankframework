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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeLine, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult, PipeRunException> chain) throws PipeRunException {
		PipeRunResult pipeRunResult;
		String objectId;
		Locker locker = pipe.getLocker();
		if (locker == null) {
			return chain.apply(message);
		}

		try {
			objectId = locker.acquire();
		} catch (Exception e) {
			throw new PipeRunException(pipe, "error while trying to obtain lock [" + locker + "]", e);
		}
		if (objectId == null) {
			throw new PipeRunException(pipe, "could not obtain lock [" + locker + "]");
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
		return pipeRunResult;
	}

}
