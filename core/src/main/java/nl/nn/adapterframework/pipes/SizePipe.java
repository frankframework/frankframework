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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;

/**
 * Returns the number of bytes or characters in the input.
 *
 * @author Jaco de Groot (***@dynasol.nl)
 *
 */
public class SizePipe extends FixedForwardPipe {

	/**
	 * @see nl.nn.adapterframework.core.IPipe#doPipe(Message, PipeLineSession)
	 */
	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			int size = -1;
			Object input = message.asObject();
			if (input instanceof String) {
				size = ((String)input).length();
			} else if (input instanceof byte[]) {
				size = ((byte[])input).length;
			}
			return new PipeRunResult(getForward(), "" + size);
		} catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming input", e);
		}
	}

}
