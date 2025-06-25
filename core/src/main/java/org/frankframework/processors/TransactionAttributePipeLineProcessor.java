/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2022-2025 WeAreFrank!

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

import org.springframework.transaction.PlatformTransactionManager;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.IbisTransaction;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.stream.Message;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.ClassUtils;

/**
 * @author Jaco de Groot
 */
public class TransactionAttributePipeLineProcessor extends AbstractPipeLineProcessor {

	private @Getter @Setter PlatformTransactionManager txManager;

	@Override
	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId, Message message, PipeLineSession pipeLineSession, String firstPipe) throws PipeRunException {
		try {
			IbisTransaction itx = new IbisTransaction(txManager, pipeLine.getTxDef(), "pipeline of adapter [" + pipeLine.getAdapter().getName() + "]");
			try {
				TimeoutGuard tg = new TimeoutGuard("pipeline of adapter [" + pipeLine.getAdapter().getName() + "]");
				Throwable tCaught=null;
				try {
					tg.activateGuard(pipeLine.getTransactionTimeout());
					PipeLineResult pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession, firstPipe);

					boolean mustRollback=false;

					if (pipeLineResult==null) {
						mustRollback=true;
						log.warn("Pipeline received null result for messageId [{}], transaction (when present and active) will be rolled back", messageId);
					} else {
						if (!pipeLineResult.isSuccessful()) {
							mustRollback=true;
							log.warn("Pipeline result state [{}] for messageId [{}] is not equal to [{}], transaction (when present and active) will be rolled back", pipeLineResult.getState(), messageId, ExitState.SUCCESS);
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
						log.warn("Thread interrupted, but propagating other caught exception of type [{}]", ClassUtils.nameOf(tCaught));
					}
				}
			} catch (Throwable t) {
				log.debug("setting RollBackOnly for pipeline after catching exception");
				itx.setRollbackOnly();
				if (t instanceof Error error) {
					throw error;
				} else if (t instanceof RuntimeException exception) {
					throw exception;
				} else if (t instanceof PipeRunException exception) {
					throw exception;
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
