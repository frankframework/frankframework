/*
 * $Log: SpringTxPipeLineExecutor.java,v $
 * Revision 1.5  2008-01-11 10:06:05  europe\L190409
 * some rework
 *
 * Revision 1.4  2007/10/17 08:22:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Always commit 'own' transaction-status
 *
 * Revision 1.3  2007/10/17 08:14:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Add extra log statements
 *
 * Revision 1.2  2007/10/09 15:54:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD of txSupport classes
 *
 */
package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.util.SpringTxManagerProxy;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Implementation of interface IPipeLineExecutor that uses a transaction manager provided 
 * by the Spring framework to ensure the method is executed in the right transaction state.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class SpringTxPipeLineExecutor extends SpringTxExecutorBase implements IPipeLineExecutor {

	public PipeLineResult doPipeLineTransactional(int propagation, PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
		return doPipeLineTransactional(SpringTxManagerProxy.getTransactionDefinition(propagation),pipeLine,messageId, message, session);
	}

    public PipeLineResult doPipeLineTransactional(TransactionDefinition txDef, PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        TransactionStatus txStatus = txManager.getTransaction(txDef);
		if (log.isDebugEnabled()) log.debug("doPipeLineTransactional with TX-definition " + txDef + ", txStatus: new="
                + txStatus.isNewTransaction() + ", rollback-only:" + txStatus.isRollbackOnly());
        try {
            return pipeLine.processPipeLine(messageId, message, session, txStatus);
        } catch (Throwable t) {
			if (log.isDebugEnabled()) log.debug("setting RollBackOnly for pipeline");
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
			if (log.isDebugEnabled()) log.debug("Performing commit/rollback on transaction " + txStatus);
			txManager.commit(txStatus);
        }
    }

}
