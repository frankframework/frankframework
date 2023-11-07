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

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.doc.ElementType;
import nl.nn.adapterframework.doc.ElementType.ElementTypes;
import nl.nn.adapterframework.stream.Message;

/**
 * Pipe that throws an exception, based on the input message.
 * 
 * @author  Gerrit van Brakel
 */
@ElementType(ElementTypes.ERRORHANDLING)
public class ExceptionPipe extends FixedForwardPipe {

	private boolean throwException = true;

	@Override
	public PipeRunResult doPipe(Message message, PipeLineSession session) throws PipeRunException {

		String errorMessage;
		try {
			errorMessage = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot open stream", e);
		}
		if (StringUtils.isEmpty(errorMessage)) {
			errorMessage="exception: "+getName();
		}

		if (isThrowException()) {
			throw new PipeRunException(this, errorMessage);
		}
		log.error(errorMessage);
		return new PipeRunResult(getSuccessForward(), errorMessage);
	}


	/**
	 * when <code>true</code>, a piperunexception is thrown. otherwise the output is only logged as an error (and no rollback is performed).
	 * @ff.default true
	 */
	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}

}