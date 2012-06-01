/*
 * $Log: TransactionAttributePipeProcessor.java,v $
 * Revision 1.6  2012-06-01 10:52:49  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.5  2011/11/30 13:51:53  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:50  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2010/09/13 14:03:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now extends baseclass
 *
 * Revision 1.2  2010/09/07 15:55:13  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Removed IbisDebugger, made it possible to use AOP to implement IbisDebugger functionality.
 *
 */
package nl.nn.adapterframework.processors;

import nl.nn.adapterframework.core.HasTransactionAttribute;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * @author Jaco de Groot
 * @version Id
 */
public class TransactionAttributePipeProcessor extends PipeProcessorBase {

	private PlatformTransactionManager txManager;
	
	public PipeRunResult processPipe(PipeLine pipeLine, IPipe pipe,
			String messageId, Object message, IPipeLineSession pipeLineSession
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

	public void setTxManager(PlatformTransactionManager txManager) {
		this.txManager = txManager;
	}
	public PlatformTransactionManager getTxManager() {
		return txManager;
	}
}
