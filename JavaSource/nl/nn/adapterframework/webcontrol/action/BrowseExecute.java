/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.5  2007-11-22 09:15:32  europe\L190409
 * switch to Spring transaction manager
 *
 * Revision 1.4.2.1  2007/10/16 14:18:09  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Apply changes required to use Spring based JmsListener, Receiver and to disable JtaUtil for commits, tx status checking
 *
 * Revision 1.3.4.2  2007/10/15 09:51:58  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add back transaction-management to BrowseExecute action
 *
 * Revision 1.3.4.1  2007/09/21 09:20:33  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Remove UserTransaction from Adapter
 * * Remove InProcessStorage; refactor a lot of code in Receiver
 *
 * Revision 1.3  2005/09/29 14:57:11  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * split transactional storage browser in browse-only and processing-options
 *
 * Revision 1.2  2005/07/28 07:44:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework using primitive types only for parameters
 *
 * Revision 1.1  2005/07/19 15:29:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of BrowseExcecute
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionError;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Extension to transactionalstorage browser, that enables delete and repost.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class BrowseExecute extends Browse {
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.5 $ $Date: 2007-11-22 09:15:32 $";
    
    protected static final TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    
	protected void performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId) {
		TransactionStatus txStatus = null;
        PlatformTransactionManager transactionManager = ibisManager.getTransactionManager();
        
        if ("deletemessage".equalsIgnoreCase(action)) {
			if (StringUtils.isNotEmpty(messageId)) {
				try {
					txStatus = transactionManager.getTransaction(TXNEW);
                    mb.deleteMessage(messageId);
                    transactionManager.commit(txStatus);
				} catch (Throwable e) {
                    if (txStatus != null && !txStatus.isCompleted()) {
                        try {
                            transactionManager.rollback(txStatus);
                        } catch (TransactionException te) {
                            log.error("Error rolling back transaction after original error, transaction rollback error:", te);
                        }
                    }
					log.error("Error occurred performing action '" + action + "'", e);
					errors.add("",
						new ActionError(
							"errors.generic",
							"error occured deleting message:" + e.getMessage()));
				}
			}
		} 

		if ("resendmessage".equalsIgnoreCase(action)) {
			if (StringUtils.isNotEmpty(messageId)) {
				try {
					txStatus = transactionManager.getTransaction(TXNEW);
					Object msg = mb.getMessage(messageId);
					if (msg==null) {
						errors.add("",
							new ActionError(
								"errors.generic",
								"did not retrieve message" ));
                        transactionManager.rollback(txStatus);
					} else {
						if (adapter.getRunState()!=RunStateEnum.STARTED) {
							errors.add("",
								new ActionError(
									"errors.generic",
									"message not processed: adapter not open" ));
                            transactionManager.rollback(txStatus);
						} else {
							receiver.processRawMessage(receiver.getListener(),msg,null,-1);
                            if (!txStatus.isCompleted()) {
                                transactionManager.commit(txStatus);
                            }
						}
					}
				} catch (Throwable e) {
                    if (txStatus != null && !txStatus.isCompleted()) {
                        try {
                            transactionManager.rollback(txStatus);
                        } catch (TransactionException te) {
                            log.error("Error rolling back transaction after original error, transaction rollback error:", te);
                        }
                    }
					log.error("Error occurred performing action '" + action + "'", e);
					errors.add("",
						new ActionError(
							"errors.generic",
							"error occured sending message:" + e.getMessage()));
				}
			}
		} 
	}


}
