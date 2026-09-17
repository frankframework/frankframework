/*
   Copyright 2013, 2020 Nationale-Nederlanden, 2021, 2022-2026 WeAreFrank!

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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.core.IbisTransaction;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.receivers.Receiver;
import org.frankframework.stream.Message;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.ClassUtils;

/**
 * @author Jaco de Groot
 */
public class TransactionAttributePipeLineProcessor extends AbstractPipeLineProcessor {

	private @Getter @Setter PlatformTransactionManager txManager;

	@Override
	@SuppressWarnings({ "java:S1141", "java:S1181", "java:S2629", "java:S1143", "java:S1163" }) // java:S2629: log arguments not evaluated lazily, b/c non-final variable; java:S1141: Ignore nested try-catch blocks; java:S1143, java:S1163: We want to throw from finally, sorry. java:S1181: Catching Throwable. Because we want to add the Throwable to suppressedExceptions.
	public @NonNull PipeLineResult processPipeLine(@Nullable Receiver<?> receiver, @NonNull PipeLine pipeLine, @NonNull String messageId, @NonNull Message message, @NonNull PipeLineSession pipeLineSession, @NonNull String firstPipe) throws PipeRunException {
		try {
			IbisTransaction itx = new IbisTransaction(txManager, pipeLine.getTxDef(), "pipeline of adapter [" + pipeLine.getAdapter().getName() + "]");
			try {
				TimeoutGuard tg = new TimeoutGuard("pipeline of adapter [" + pipeLine.getAdapter().getName() + "]");
				Throwable tThrown = null;
				try {
					tg.activateGuard(pipeLine.getTransactionTimeout());
					PipeLineResult pipeLineResult = pipeLineProcessor.processPipeLine(receiver, pipeLine, messageId, message, pipeLineSession, firstPipe);

					boolean mustRollback=false;

					if (!pipeLineResult.isSuccessful()) {
						mustRollback = true;
						log.warn("Pipeline result state [{}] for messageId [{}] is not equal to [{}], transaction (when present and active) will be rolled back", pipeLineResult.getState(), messageId, ExitState.SUCCESS);
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
					tThrown = t;
					throw tThrown;
				} finally {
					if (tg.cancel()) {
						if (tThrown==null) {
							throw new InterruptedException(tg.getDescription()+" was interrupted");
						}
						log.warn("Thread interrupted, but propagating other caught exception of type [{}]", ClassUtils.nameOf(tThrown));
					}
				}
			} catch (Throwable t) {
				log.debug("setting RollBackOnly for pipeline after catching exception");
				itx.setRollbackOnly();
				switch (t) {
					case Error error -> throw error;
					case RuntimeException exception -> throw exception;
					case PipeRunException exception -> throw exception;
					default -> {
						if (t instanceof InterruptedException) {
							Thread.currentThread().interrupt();
						}
						throw new PipeRunException(null, "Caught unknown checked exception", t);
					}
				}
			} finally {
				// txManager.commit(txStatus);
				itx.complete();
			}
		} catch (RuntimeException e) {
			throw new PipeRunException(null, "RuntimeException calling PipeLine with tx attribute ["
				+ pipeLine.getTransactionAttribute() + "]", e);
		}
	}

}
