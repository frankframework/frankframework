/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.HasTransactionAttribute;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.functional.ThrowingFunction;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.jta.SpringTxManagerProxy;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * @author Jaco de Groot
 */
public class TransactionAttributePipeProcessor extends PipeProcessorBase {

	private @Getter @Setter PlatformTransactionManager txManager;

	@Override
	protected PipeRunResult processPipe(PipeLine pipeline, IPipe pipe, Message message, PipeLineSession pipeLineSession, ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		TransactionDefinition txDef;
		int txTimeout = 0;
		if(pipe instanceof HasTransactionAttribute) {
			HasTransactionAttribute taPipe = (HasTransactionAttribute) pipe;
			txDef = taPipe.getTxDef();
			txTimeout = taPipe.getTransactionTimeout();
		} else {
			txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS, txTimeout);
		}
		IbisTransaction itx = new IbisTransaction(txManager, txDef, "pipe [" + pipe.getName() + "]");
		boolean isJdbcFacade = pipe instanceof MessageSendingPipe && ((MessageSendingPipe) pipe).getSender() instanceof JdbcFacade;
		try {
			if(isJdbcFacade && itx.isRollbackOnly()) {
				throw new PipeRunException(pipe, "unable to execute SQL statement, transaction has been marked as failed by an earlier sender");
			}

			return execute(pipeline, pipe, message, chain, txTimeout);

		} catch (Error | RuntimeException | PipeRunException ex) {
			if(isJdbcFacade) {
				itx.setRollbackOnly();
			}
			throw ex;
		} catch (Exception e) {
			if(isJdbcFacade) {
				itx.setRollbackOnly();
			}
			throw new PipeRunException(pipe, "Caught unknown checked exception", e);
		} finally {
			itx.complete();
		}
	}

	private PipeRunResult execute(PipeLine pipeLine, IPipe pipe, Message message, ThrowingFunction<Message, PipeRunResult, PipeRunException> chain, int txTimeout) throws Exception {
		TimeoutGuard tg = new TimeoutGuard("pipeline of adapter [" + pipeLine.getOwner().getName() + "] running pipe ["+pipe.getName()+"]");
		Exception tCaught = null;
		try {
			tg.activateGuard(txTimeout);
			return chain.apply(message);
		} catch (PipeRunException t) {
			tCaught = t;
			throw tCaught;
		} finally {
			if(tg.cancel()) {
				if(tCaught == null) {
					throw new PipeRunException(pipe, tg.getDescription() + " was interrupted");
				}
				log.warn("Thread interrupted, but propagating other caught exception of type [{}]", ClassUtils.nameOf(tCaught));
			}
		}
	}
}
