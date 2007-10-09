/*
 * $Log: SpringTxPipeExecutor.java,v $
 * Revision 1.2  2007-10-09 15:54:43  europe\L190409
 * Direct copy from Ibis-EJB:
 * first version in HEAD of txSupport classes
 *
 */
package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

/**
 * Implementation of interface IPipeExecutor that uses a transaction manager provided 
 * by the Spring framework to ensure the method is executed in the right transaction state.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class SpringTxPipeExecutor extends SpringTxExecutorBase implements IPipeExecutor {

    protected PipeRunResult doPipeTransactional(TransactionDefinition txDef, IPipe pipe, Object input, PipeLineSession session) throws PipeRunException {
        TransactionStatus txStatus = txManager.getTransaction(txDef);
        log.debug("doPipeTransactional with TX-definition " + txDef + ", txStatus: new="
                + txStatus.isNewTransaction() + ", rollback-only:" + txStatus.isRollbackOnly());
        try {
            return pipe.doPipe(input, session);
        } catch (Throwable t) {
            log.debug("Rolling back pipe " + pipe.getName());
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
            if (txStatus.isNewTransaction() && !txStatus.isCompleted()) {
                log.debug("Performing commit/rollback on transaction " + txStatus);
                txManager.commit(txStatus);
            }
        }
    }

    public PipeRunResult doPipeTxRequired(IPipe pipe, Object input, PipeLineSession session) throws PipeRunException {
        return doPipeTransactional(TXREQUIRED, pipe, input, session);
    }

    public PipeRunResult doPipeTxMandatory(IPipe pipe, Object input, PipeLineSession session) throws PipeRunException {
        return doPipeTransactional(TXMANDATORY, pipe, input, session);
    }

    public PipeRunResult doPipeTxRequiresNew(IPipe pipe, Object input, PipeLineSession session) throws PipeRunException {
        return doPipeTransactional(TXNEW, pipe, input, session);
    }

    public PipeRunResult doPipeTxSupports(IPipe pipe, Object input, PipeLineSession session) throws PipeRunException {
        return doPipeTransactional(TXSUPPORTS, pipe, input, session);
    }

    public PipeRunResult doPipeTxNotSupported(IPipe pipe, Object input, PipeLineSession session) throws PipeRunException {
        return doPipeTransactional(TXNOTSUPPORTED, pipe, input, session);
    }

    public PipeRunResult doPipeTxNever(IPipe pipe, Object input, PipeLineSession session) throws PipeRunException {
        return doPipeTransactional(TXNEVER, pipe, input, session);
    }
}
