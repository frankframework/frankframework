/*
 * $Log: TransactionAttributePipeProcessor.java,v $
 * Revision 1.2  2010-09-07 15:55:13  m00f069
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.HasTransactionAttribute;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class TransactionAttributePipeProcessor extends TransactionAttributeProcessor implements PipeProcessor {
	private PipeProcessor pipeProcessor;

	public void setPipeProcessor(PipeProcessor pipeProcessor) {
		this.pipeProcessor = pipeProcessor;
	}
	
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, PipeLineSession pipeLineSession
			) throws PipeRunException {
		PipeRunResult pipeRunResult;
		int txOption;
		int txTimeout=0;
		if (pipe instanceof HasTransactionAttribute) {
			HasTransactionAttribute taPipe = (HasTransactionAttribute) pipe;
			txOption = taPipe.getTransactionAttributeNum();
			txTimeout= taPipe.getTransactionTimeout();
		} else {
			txOption = TransactionDefinition.PROPAGATION_SUPPORTS;
		}
		//TransactionStatus txStatus = txManager.getTransaction(SpringTxManagerProxy.getTransactionDefinition(txOption,txTimeout));
		IbisTransaction itx = new IbisTransaction(txManager, SpringTxManagerProxy.getTransactionDefinition(txOption,txTimeout), "pipe [" + pipe.getName() + "]");
		TransactionStatus txStatus = itx.getStatus();
		try {
			TimeoutGuard tg = new TimeoutGuard("pipeline of adapter [" + pipeLine.getOwner().getName() + "] running pipe ["+pipe.getName()+"]");
			Throwable tCaught=null;
			try {
				tg.activateGuard(txTimeout);
				pipeRunResult = pipeProcessor.processPipe(pipeLine, pipe, messageId, message, pipeLineSession);
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
			txStatus.setRollbackOnly();
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
			//txManager.commit(txStatus);
			itx.commit();
		}
		return pipeRunResult;
	}

}
