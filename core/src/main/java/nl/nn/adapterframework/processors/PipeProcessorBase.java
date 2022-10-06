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
package nl.nn.adapterframework.processors;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IValidator;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;

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
