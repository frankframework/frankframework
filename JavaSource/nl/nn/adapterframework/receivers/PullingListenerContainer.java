/*
 * $Log: PullingListenerContainer.java,v $
 * Revision 1.3  2007-10-16 13:02:09  europe\M00035F
 * Add ReceiverBaseSpring from EJB branch
 *
 * Revision 1.2  2007/10/10 08:53:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Direct copy from Ibis-EJB:
 * first version in HEAD
 *
 */
package nl.nn.adapterframework.receivers;

import java.util.HashMap;
import java.util.Map;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.Counter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.Semaphore;
import nl.nn.adapterframework.util.TracingUtil;
import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * 
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */

public class PullingListenerContainer implements Runnable {
	protected Logger log = LogUtil.getLogger(this);

    private final static TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    ReceiverBaseSpring receiver;
    PlatformTransactionManager txManager;
    private Counter threadsRunning = new Counter(0);
    private Semaphore pollToken = null;
    private int retryInterval;
    
    private PullingListenerContainer() {
        super();
    }
    
    public void configure() {
        if (receiver.getNumThreadsPolling()>0 && receiver.getNumThreadsPolling()<receiver.getNumThreads()) {
            pollToken = new Semaphore(receiver.getNumThreadsPolling());
        }
    }
    
	/**
	 * Starts the receiver. This method is called by the startRunning method.<br/>
	 * Basically:
	 * <ul>
	 * <li>it calls the getRawMessage method to get a message<li>
	 * <li> it performs the onMessage method, resulting a PipeLineResult</li>
	 * <li>it calls the afterMessageProcessed() method of the listener<li>
	 * <li> it optionally sends the result using the sender</li>
	 * </ul>
	 */
    public void run() {
        threadsRunning.increase();
        Thread.currentThread().setName(receiver.getName()+"-listener["+threadsRunning.getValue()+"]");
        IPullingListener listener = null;
        Map threadContext = null;
        try {
            listener = (IPullingListener) receiver.getListener();
            threadContext = listener.openThread();
            if (threadContext == null) {
                threadContext = new HashMap();
            }
            long startProcessingTimestamp;
            long finishProcessingTimestamp = System.currentTimeMillis();
            receiver.setRunState(RunStateEnum.STARTED);
            while (receiver.isInRunState(RunStateEnum.STARTED)) {
                boolean permissionToGo = true;
                if (pollToken != null) {
                    try {
                        permissionToGo = false;
                        pollToken.acquire();
                        permissionToGo = true;
                    } catch (Exception e) {
                        receiver.error("acquisition of polltoken interupted", e);
                        receiver.stopRunning();
                    }
                }
                Object rawMessage = null;
                TransactionStatus txStatus = null;
                try {
                    if (permissionToGo && receiver.isInRunState(RunStateEnum.STARTED)) {
                        try {
                            if (receiver.isTransacted()) {
                                txStatus = txManager.getTransaction(TXNEW);
                            }
                            rawMessage = listener.getRawMessage(threadContext);
                            synchronized (listener) {
                                retryInterval = 1;
                            }
                        } catch (Exception e) {
                            if (receiver.isOnErrorStop()) {
                                long currentInterval;
                                synchronized (listener) {
                                    currentInterval = retryInterval;
                                    retryInterval = retryInterval * 2;
                                    if (retryInterval > 3600) {
                                        retryInterval = 3600;
                                    }
                                }
                                receiver.error("caught Exception retrieving message, will continue retrieving messages in [" + currentInterval + "] seconds", e);
                                while (receiver.isInRunState(RunStateEnum.STARTED) && currentInterval-- > 0) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (Exception e2) {
                                        receiver.error("sleep interupted", e2);
                                        receiver.stopRunning();
                                    }
                                }
                            } else {
                                receiver.error("stopping receiver after exception in retrieving message", e);
                                receiver.stopRunning();
                            }
                        }
                    }
                } finally {
                    if (pollToken != null) {
                        pollToken.release();
                    }
                }
                if (rawMessage != null) {
                    try {
                        TracingUtil.beforeEvent(this);
                        startProcessingTimestamp = System.currentTimeMillis();
                        try {
                            receiver.processRawMessage(listener, rawMessage, threadContext, finishProcessingTimestamp - startProcessingTimestamp);
                            if (txStatus != null) {
                                txManager.commit(txStatus);
                            }
                        } catch (Exception e) {
                            TracingUtil.exceptionEvent(this);
                            if (txStatus != null && !txStatus.isCompleted()) {
                                txManager.rollback(txStatus);
                            }
                            if (receiver.isOnErrorStop()) {
                                receiver.error("caught Exception processing message, will continue processing next message", e);
                            } else {
                                receiver.error("stopping receiver after exception in processing message", e);
                                receiver.stopRunning();
                            }
                        }
                        finishProcessingTimestamp = System.currentTimeMillis();
                    } finally {
                        TracingUtil.afterEvent(this);
                    }
                } else {
                    if (txStatus != null && !txStatus.isCompleted()) {
                        txManager.rollback(txStatus);
                    }
                }
            }
        } catch (Throwable e) {
            receiver.error("error occured in receiver [" + receiver.getName() + "]", e);
        } finally {
            if (listener != null) {
                try {
                    listener.closeThread(threadContext);
                } catch (ListenerException e) {
                    receiver.error("Exception closing listener of Receiver [" + receiver.getName() + "]", e);
                }
            }
            long stillRunning = threadsRunning.decrease();
            if (stillRunning > 0) {
                log.info("a thread of Receiver [" + receiver.getName() + "] exited, [" + stillRunning + "] are still running");
                return;
            }
            log.info("the last thread of Receiver [" + receiver.getName() + "] exited, cleaning up");
            receiver.closeAllResources();
        }
    }

    public ReceiverBaseSpring getReceiver() {
        return receiver;
    }

    public void setReceiver(ReceiverBaseSpring receiver) {
        this.receiver = receiver;
    }
    
}
