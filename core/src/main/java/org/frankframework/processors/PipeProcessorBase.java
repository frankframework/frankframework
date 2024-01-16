/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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

import org.apache.logging.log4j.Logger;

import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.stream.Message;
import org.frankframework.util.LogUtil;

/**
 * Baseclass for PipeProcessors.
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public abstract class PipeProcessorBase implements PipeProcessor {
	protected Logger log = LogUtil.getLogger(this);

	private PipeProcessor pipeProcessor;

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}

	protected abstract PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException;

	@Override
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, PipeLineSession pipeLineSession) throws PipeRunException {
		return processPipe(pipeLine, pipe, message, pipeLineSession, m -> pipeProcessor.processPipe(pipeLine, pipe, m, pipeLineSession));
	}

	@Override
	public PipeRunResult validate(PipeLine pipeLine, IValidator validator, Message message, PipeLineSession pipeLineSession, String messageRoot) throws PipeRunException {
		return processPipe(pipeLine, validator, message, pipeLineSession, m -> pipeProcessor.validate(pipeLine, validator, m, pipeLineSession, messageRoot));
	}

}
