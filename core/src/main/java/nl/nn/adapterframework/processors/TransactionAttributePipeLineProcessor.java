/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2022 WeAreFrank!

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

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;

/**
 * @author Jaco de Groot
 */
public class TransactionAttributePipeLineProcessor extends PipeLineProcessorBase {

	private @Getter @Setter PlatformTransactionManager txManager;

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		try {
			//TransactionStatus txStatus = txManager.getTransaction(txDef);
			IbisTransaction itx = new IbisTransaction(txManager, pipeLine.getTxDef(), "pipeline of adapter [" + pipeLine.getOwner().getName() + "]");
			try {
				TimeoutGuard tg = new TimeoutGuard("pipeline of adapter [" + pipeLine.getOwner().getName() + "]");
				Throwable tCaught=null;
				try {
					tg.activateGuard(pipeLine.getTransactionTimeout());
					PipeLineResult pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);

					boolean mustRollback=false;

					if (pipeLineResult==null) {
						mustRollback=true;
						log.warn("Pipeline received null result for messageId ["+messageId+"], transaction (when present and active) will be rolled back");
					} else {
						if (!pipeLineResult.isSuccessful()) {
							mustRollback=true;
							log.warn("Pipeline result state ["+pipeLineResult.getState()+"] for messageId ["+messageId+"] is not equal to ["+ExitState.SUCCESS+"], transaction (when present and active) will be rolled back");
						}
					}
					if (mustRollback) {
						try {
							itx.setRollbackOnly();
						} catch (Exception e) {
							throw new PipeRunException(null,"Could not set RollBackOnly",e);
						}
					}

					return pipeLineResult;
				} catch (Throwable t) {
					tCaught=t;
					throw tCaught;
				} finally {
					if (tg.cancel()) {
						if (tCaught==null) {
							throw new InterruptedException(tg.getDescription()+" was interrupted");
						}
						log.warn("Thread interrupted, but propagating other caught exception of type ["+ClassUtils.nameOf(tCaught)+"]");
					}
				}
			} catch (Throwable t) {
				log.debug("setting RollBackOnly for pipeline after catching exception");
				itx.setRollbackOnly();
				if (t instanceof Error) {
					throw (Error)t;
				} else if (t instanceof RuntimeException) {
					throw (RuntimeException)t;
				} else if (t instanceof PipeRunException) {
					throw (PipeRunException)t;
				} else {
					throw new PipeRunException(null, "Caught unknown checked exception", t);
				}
			} finally {
				//txManager.commit(txStatus);
				itx.complete();
			}
		} catch (RuntimeException e) {
			throw new PipeRunException(null, "RuntimeException calling PipeLine with tx attribute ["
				+ pipeLine.getTransactionAttribute() + "]", e);
		}
	}

}
