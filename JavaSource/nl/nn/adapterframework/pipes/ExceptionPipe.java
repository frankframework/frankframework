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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.apache.commons.lang.StringUtils;

/**
 * Pipe that throws an exception, based on the input message.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setThrowException(boolean) throwException}</td><td>when <code>true</code>, a PipeRunException is thrown. Otherwise the output is only logged as an error (and no rollback is performed).</td><td>true</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified</td></tr>
 * </table>
 * </p>
 * 
 * @author  Gerrit van Brakel
 * @version $Id$
 */

public class ExceptionPipe extends FixedForwardPipe {

	private boolean throwException = true;

	public PipeRunResult doPipe(Object input, IPipeLineSession session)
		throws PipeRunException {

		String message = (String)input;
		if (StringUtils.isEmpty(message)) {
			message="exception: "+getName();
		}

		if (isThrowException())
			throw new PipeRunException(this, message);
		else {
			log.error(message);
			return new PipeRunResult(getForward(), message);
		}
	}


	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}

}