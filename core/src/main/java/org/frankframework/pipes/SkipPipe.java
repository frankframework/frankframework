/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2022, 2024 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;

/**
 * Skip a number of bytes or characters from the input message.
 *
 * @author Jaco de Groot (***@dynasol.nl)
 *
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class SkipPipe extends FixedForwardPipe {

	private int skip = 0;
	private int length = -1;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {
		try {
			Message result;
			if (!message.isBinary()) {
				String stringInput = message.asString();
				if (StringUtils.isEmpty(stringInput)) {
					return new PipeRunResult(getSuccessForward(), message);
				}
				if (skip > stringInput.length()) {
					result = new Message("");
				} else {
					if (length >= 0 && length < stringInput.length() - skip) {
						result = new Message(stringInput.substring(skip, skip + length));
					} else {
						result = new Message(stringInput.substring(skip));
					}
				}
			} else {
				byte[] bytesInput = message.asByteArray();
				byte[] bytesResult;
				if (skip > bytesInput.length) {
					bytesResult = new byte[0];
				} else {
					if (length >= 0 && length < bytesInput.length - skip) {
						bytesResult = new byte[length];
						System.arraycopy(bytesInput, skip, bytesResult, 0, length);
					} else {
						bytesResult = new byte[bytesInput.length - skip];
						System.arraycopy(bytesInput, skip, bytesResult, 0, bytesResult.length);
					}
				}
				result = new Message(bytesResult);
			}
			return new PipeRunResult(getSuccessForward(), result);
		} catch(Exception e) {
			throw new PipeRunException(this, "Error while transforming input", e);
		}
	}

	/**
	 * Number of bytes (for binary input) or characters (for character input) to skip. An empty byte array or string is returned when skip is larger than the length of the input.
	 * @ff.default 0
	 */
	public void setSkip(int skip) {
		this.skip = skip;
	}

	/**
	 * If length>=0 only these number of bytes (for binary input) or characters (for character input) is returned.
	 * @ff.default -1
	 */
	public void setLength(int length) {
		this.length = length;
	}
}
