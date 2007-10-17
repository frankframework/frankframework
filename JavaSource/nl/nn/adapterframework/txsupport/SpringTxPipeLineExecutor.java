/*
 * $Log: SpringTxPipeLineExecutor.java,v $
 * Revision 1.4  2007-10-17 08:22:03  europe\L190409
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

    protected PipeLineResult doPipeLineTransactional(TransactionDefinition txDef, PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        TransactionStatus txStatus = txManager.getTransaction(txDef);
        log.debug("doPipeLineTransactional with TX-definition " + txDef + ", txStatus: new="
                + txStatus.isNewTransaction() + ", rollback-only:" + txStatus.isRollbackOnly());
        try {
            return pipeLine.processPipeLine(messageId, message, session);
        } catch (Throwable t) {
            log.debug("Rolling back the pipe line");
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
			//if (txStatus.isNewTransaction()) {
				if (!txStatus.isCompleted()) {
					log.debug("Performing commit/rollback on transaction " + txStatus);
					txManager.commit(txStatus);
				} else {
					log.warn("Transaction started by us already completed after pipeline-call finished");
				}
			//} else {
			//	log.debug("PipeLine call finished; transaction not started by us therefore not committing");
			//}
        }
    }

    public PipeLineResult doPipeLineTxRequired(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        return doPipeLineTransactional(TXREQUIRED, pipeLine, messageId, message, session);
    }

    public PipeLineResult doPipeLineTxMandatory(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        return doPipeLineTransactional(TXMANDATORY, pipeLine, messageId, message, session);
    }

    public PipeLineResult doPipeLineTxRequiresNew(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        return doPipeLineTransactional(TXNEW, pipeLine, messageId, message, session);
    }

    public PipeLineResult doPipeLineTxSupports(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        return doPipeLineTransactional(TXSUPPORTS, pipeLine, messageId, message, session);
    }

    public PipeLineResult doPipeLineTxNotSupported(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        return doPipeLineTransactional(TXNOTSUPPORTED, pipeLine, messageId, message, session);
    }

    public PipeLineResult doPipeLineTxNever(PipeLine pipeLine, String messageId, String message, PipeLineSession session) throws PipeRunException {
        return doPipeLineTransactional(TXNEVER, pipeLine, messageId, message, session);
    }

}
