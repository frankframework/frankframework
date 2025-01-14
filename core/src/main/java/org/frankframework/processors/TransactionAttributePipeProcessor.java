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
package org.frankframework.processors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.HasSender;
import org.frankframework.core.HasTransactionAttribute;
import org.frankframework.core.IPipe;
import org.frankframework.core.ISender;
import org.frankframework.core.IbisTransaction;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.functional.ThrowingFunction;
import org.frankframework.jdbc.JdbcFacade;
import org.frankframework.jms.JMSFacade;
import org.frankframework.jta.SpringTxManagerProxy;
import org.frankframework.stream.Message;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.ClassUtils;

/**
 * @author Jaco de Groot
 */
public class TransactionAttributePipeProcessor extends AbstractPipeProcessor {

	private @Getter @Setter PlatformTransactionManager txManager;

	@Override
	protected PipeRunResult processPipe(@Nonnull PipeLine pipeline, @Nonnull IPipe pipe, @Nullable Message message, @Nonnull PipeLineSession pipeLineSession, @Nonnull ThrowingFunction<Message, PipeRunResult,PipeRunException> chain) throws PipeRunException {
		TransactionDefinition txDef;
		int txTimeout = 0;
		if(pipe instanceof HasTransactionAttribute taPipe) {
			txDef = taPipe.getTxDef();
			txTimeout = taPipe.getTransactionTimeout();
		} else {
			txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS, txTimeout);
		}
		IbisTransaction itx = new IbisTransaction(txManager, txDef, "pipe [" + pipe.getName() + "]");
		boolean isTxCapable = hasTxCapableSender(pipe);
		try {
			if(isTxCapable && itx.isRollbackOnly()) {
				throw new PipeRunException(pipe, "unable to execute SQL statement, transaction has been marked as failed by an earlier sender");
			}

			return execute(pipeline, pipe, message, chain, txTimeout);

		} catch (Error | RuntimeException | PipeRunException ex) {
			if(isTxCapable) {
				itx.setRollbackOnly();
			}
			throw ex;
		} catch (Exception e) {
			if(isTxCapable) {
				itx.setRollbackOnly();
			}
			throw new PipeRunException(pipe, "Caught unknown checked exception", e);
		} finally {
			itx.complete();
		}
	}

	/** If the pipe implements HasSender and the sender is TX Capable, it should mark RollBackOnly! */
	private boolean hasTxCapableSender(IPipe pipe) {
		if(pipe instanceof HasSender hasSender) {
			ISender sender = hasSender.getSender();
			return sender instanceof JdbcFacade || sender instanceof JMSFacade;
		}
		return false;
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
