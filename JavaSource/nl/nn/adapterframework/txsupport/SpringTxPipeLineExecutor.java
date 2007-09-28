/*
 * SpringTxPipeLineExecutor.java
 * 
 * Created on 28-sep-2007, 10:28:54
 * 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package nl.nn.adapterframework.txsupport;

import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import org.apache.log4j.Logger;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 *
 * @author m00035f
 */
public class SpringTxPipeLineExecutor implements IPipeLineExecutor {

    public final static TransactionDefinition TXREQUIRED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRED);
    public final static TransactionDefinition TXSUPPORTS = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_SUPPORTS);
    public final static TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    public final static TransactionDefinition TXNOTSUPPORTED = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
    public final static TransactionDefinition TXMANDATORY = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_MANDATORY);
    public final static TransactionDefinition TXNEVER = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NEVER);
    
    private final static Logger log = Logger.getLogger(SpringTxPipeLineExecutor.class);

    private PlatformTransactionManager txManager;

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
            if (txStatus.isNewTransaction() && !txStatus.isCompleted()) {
                log.debug("Performing commit/rollback on transaction " + txStatus);
                txManager.commit(txStatus);
            }
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

    public PlatformTransactionManager getTxManager() {
        return txManager;
    }

    public void setTxManager(PlatformTransactionManager txManager) {
        this.txManager = txManager;
    }

}
