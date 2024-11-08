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
package org.frankframework.pipes;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.doc.EnterpriseIntegrationPattern;
import org.frankframework.doc.Forward;
import org.frankframework.stream.Message;

/**
 * Pipe that throws an exception, based on the input message.
 * <br/>
 * The {@literal success} forward is only used when the (deprecated) attribute {@literal throwException} has been set to {@literal false}, otherwise the (default) {@literal exception} forward will be used.
 *
 * @ff.warning The attribute {@literal throwException} has been deprecated and thus the {@literal success} forward will be removed along with the {@literal throwException} attribute.
 */
@EnterpriseIntegrationPattern(EnterpriseIntegrationPattern.Type.ERRORHANDLING)
@Forward(name = "success", description = "success Forward is deprecated and will be removed. Invoked when {@literal throwException} is false")
public class ExceptionPipe extends AbstractPipe {

	private boolean throwException = true;
	private PipeForward successForward;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		successForward = findForward(PipeForward.SUCCESS_FORWARD_NAME);
	}

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

		return new PipeRunResult(successForward, errorMessage);
	}


	/**
	 * When <code>true</code>, a PipeRunException is thrown. Otherwise, the output is only logged as an error (and no rollback is performed).
	 * @ff.default true
	 */
	@Deprecated(forRemoval = true, since = "9.0")
	@ConfigurationWarning(value = "The {@literal success} forward and {@literal throwException} attribute should not be used anymore")
	public void setThrowException(boolean b) {
		throwException = b;
	}
	public boolean isThrowException() {
		return throwException;
	}

}
