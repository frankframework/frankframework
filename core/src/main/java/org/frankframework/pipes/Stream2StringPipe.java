/*
   Copyright 2018, 2020 Nationale-Nederlanden

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
package org.frankframework.pipes;

import java.io.IOException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;

/**
 * Return simply the input message from stream to string.
 *
 * @author  Tom van der Heijden
 * @deprecated not necessary when using Messages.
 */
@Deprecated(forRemoval = true, since = "7.6.0")
public class Stream2StringPipe extends FixedForwardPipe {

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		String result = null;
		try {
			result = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "Could not convert stream to text", e);
		}
		return new PipeRunResult(getSuccessForward(), result);
	}
}
