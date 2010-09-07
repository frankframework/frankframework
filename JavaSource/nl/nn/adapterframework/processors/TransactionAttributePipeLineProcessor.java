/*
 * $Log: TransactionAttributePipeLineProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.TransactionStatus;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class TransactionAttributePipeLineProcessor extends TransactionAttributeProcessor implements PipeLineProcessor {
	private PipeLineProcessor pipeLineProcessor;

	public void setPipeLineProcessor(PipeLineProcessor pipeLineProcessor) {
		this.pipeLineProcessor = pipeLineProcessor;
	}

	public PipeLineResult processPipeLine(PipeLine pipeLine, String messageId,
			String message, PipeLineSession pipeLineSession
			) throws PipeRunException {
		try {
			//TransactionStatus txStatus = txManager.getTransaction(txDef);
			IbisTransaction itx = new IbisTransaction(txManager, pipeLine.getTxDef(), "pipeline of adapter [" + pipeLine.getOwner().getName() + "]");
			TransactionStatus txStatus = itx.getStatus();
			try {
				TimeoutGuard tg = new TimeoutGuard("pipeline of adapter [" + pipeLine.getOwner().getName() + "]");
				Throwable tCaught=null;
				try {
					tg.activateGuard(pipeLine.getTransactionTimeout());
					PipeLineResult pipeLineResult = pipeLineProcessor.processPipeLine(pipeLine, messageId, message, pipeLineSession);

					boolean mustRollback=false;
				
					if (pipeLineResult==null) {
						mustRollback=true;
						log.warn("Pipeline received null result for messageId ["+messageId+"], transaction (when present and active) will be rolled back");
					} else {
						if (StringUtils.isNotEmpty(pipeLine.getCommitOnState()) && !pipeLine.getCommitOnState().equalsIgnoreCase(pipeLineResult.getState())) {
							mustRollback=true;
							log.warn("Pipeline result state ["+pipeLineResult.getState()+"] for messageId ["+messageId+"] is not equal to commitOnState ["+pipeLine.getCommitOnState()+"], transaction (when present and active) will be rolled back");
						}
					}
					if (mustRollback) {
						try {
							txStatus.setRollbackOnly();
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
						} else {
							log.warn("Thread interrupted, but propagating other caught exception of type ["+ClassUtils.nameOf(tCaught)+"]");
						}
					}
				}
			} catch (Throwable t) {
				log.debug("setting RollBackOnly for pipeline after catching exception");
				txStatus.setRollbackOnly();
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
				itx.commit();
			}
		} catch (RuntimeException e) {
			throw new PipeRunException(null, "RuntimeException calling PipeLine with tx attribute ["
				+ pipeLine.getTransactionAttribute() + "]", e);
		}
	}

}
