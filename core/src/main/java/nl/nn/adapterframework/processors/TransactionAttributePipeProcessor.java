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

import nl.nn.adapterframework.core.HasTransactionAttribute;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

/**
 * @author Jaco de Groot
 */
public class TransactionAttributePipeProcessor extends PipeProcessorBase {

	private PlatformTransactionManager txManager;
	
	@Override
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe, Message message, IPipeLineSession pipeLineSession) throws PipeRunException {
		PipeRunResult pipeRunResult;
		TransactionDefinition txDef;
		int txTimeout=0;
		if (pipe instanceof HasTransactionAttribute) {
			HasTransactionAttribute taPipe = (HasTransactionAttribute) pipe;
			txDef = taPipe.getTxDef();
			txTimeout= taPipe.getTransactionTimeout();
		} else {
			txDef = SpringTxManagerProxy.getTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS,txTimeout);
		}
		IbisTransaction itx = new IbisTransaction(txManager, txDef, "pipe [" + pipe.getName() + "]");
		try {
			TimeoutGuard tg = new TimeoutGuard("pipeline of adapter [" + pipeLine.getOwner().getName() + "] running pipe ["+pipe.getName()+"]");
			Throwable tCaught=null;
			try {
				tg.activateGuard(txTimeout);
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, message, pipeLineSession);
			} catch (Throwable t) {
				tCaught=t;
				throw tCaught;
			} finally {
				if (tg.cancel()) {
					if (tCaught==null) {
						throw new PipeRunException(pipe,tg.getDescription()+" was interrupted");
					} else {
						log.warn("Thread interrupted, but propagating other caught exception of type ["+ClassUtils.nameOf(tCaught)+"]");
					}
				}
			}
		} catch (Throwable t) {
			log.debug("setting RollBackOnly for pipe [" + pipe.getName()+"] after catching exception");
			itx.setRollbackOnly();
			if (t instanceof Error) {
				throw (Error)t;
			} else if (t instanceof RuntimeException) {
				throw (RuntimeException)t;
			} else if (t instanceof PipeRunException) {
				throw (PipeRunException)t;
			} else {
				throw new PipeRunException(pipe, "Caught unknown checked exception", t);
			}
		} finally {
			itx.commit();
		}
		return pipeRunResult;
	}

	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}
}
