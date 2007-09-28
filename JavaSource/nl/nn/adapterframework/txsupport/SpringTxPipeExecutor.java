/*
 * SpringTxPipeExecutor.java
 * 
 * Created on 28-sep-2007, 10:16:35
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import org.apache.log4j.Logger;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 *
 * @author m00035f
 */
public class SpringTxPipeExecutor implements IPipeExecutor {
    public final static TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
    public final static TransactionDefinition TXSUPPORTS = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS);
    public final static TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    public final static TransactionDefinition TXNOTSUPPORTED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    public final static TransactionDefinition TXMANDATORY = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY);
    public final static TransactionDefinition TXNEVER = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NEVER);
    
    private final static Logger log = Logger.getLogger(SpringTxPipeExecutor.class);

    private PlatformTransactionManager txManager;

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

    public PlatformTransactionManager getTxManager() {
        return txManager;
    }

    public void setTxManager(PlatformTransactionManager txManager) {
        this.txManager = txManager;
    }

}
