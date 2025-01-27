/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2022 WeAreFrank!

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

import lombok.Getter;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.stream.Message;
import org.frankframework.util.UUIDUtil;

/**
 * Pipe that generates an UUID (Universally Unique Identifier).
 *
 * Only type <code>alphanumeric</code> guarantees a 100% unique identifier, type <code>numeric</code> has a 0.01% chance of exactly the same id in case of multiple calls on the same host within a few milliseconds.
 *
 * @author Peter Leeuwenburgh
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.TRANSLATOR)
public class UUIDGeneratorPipe extends FixedForwardPipe {

	private @Getter Type type = Type.ALPHANUMERIC;

	public enum Type {
		/** the UUID will not have a fixed length which will be about 42 */
		ALPHANUMERIC,
		/** a UUID with fixed length 31 will be generated */
		NUMERIC
	}

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		String result = null;
		if (getType()==Type.ALPHANUMERIC) {
			result = UUIDUtil.createUUID();
		} else {
			result = UUIDUtil.createNumericUUID();
		}

		return new PipeRunResult(getSuccessForward(), result);
	}

	/**
	 * Format of generated string.
	 * @ff.default alphanumeric
	 */
	public void setType(Type value) {
		this.type = value;
	}
}
