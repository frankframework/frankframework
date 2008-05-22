/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.8.2.1  2008-05-22 14:36:57  europe\L190409
 * sync from HEAD
 *
 * Revision 1.9  2008/05/22 07:32:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use inherited error() method
 *
 * Revision 1.8  2008/02/08 09:49:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * log errors for retry attempts
 *
 * Revision 1.7  2008/01/11 14:56:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * resend using retry function of receiver
 *
 * Revision 1.6  2007/12/10 10:25:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework
 *
 * Revision 1.5  2007/11/22 09:15:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
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
import nl.nn.adapterframework.util.ClassUtils;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
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
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.8.2.1 $ $Date: 2008-05-22 14:36:57 $";
    
    protected static final TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    
	protected void performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId, String selected[]) {
        PlatformTransactionManager transactionManager = ibisManager.getTransactionManager();
		log.debug("retrieved transactionManager ["+ClassUtils.nameOf(transactionManager)+"]["+transactionManager+"] from ibismanager ["+ibisManager+"]");

		log.debug("performing action ["+action+"]");
		try {
       
	        if ("deletemessage".equalsIgnoreCase(action)) {
				if (StringUtils.isNotEmpty(messageId)) {
					deleteMessage(mb,messageId,transactionManager);
				}
			} 
	
			if ("delete selected".equalsIgnoreCase(action)) {
				for(int i=0; i<selected.length; i++) {
					try {
						deleteMessage(mb,selected[i],transactionManager);
					} catch (Throwable e) {
						error(", ", "errors.generic", "Could not delete message with id ["+selected[i]+"]", e);
					}
				}
			}
	
			if ("resendmessage".equalsIgnoreCase(action)) {
				if (StringUtils.isNotEmpty(messageId)) {
					receiver.retryMessage(messageId);
				}
			}
			
			if ("resend selected".equalsIgnoreCase(action)) {
				for(int i=0; i<selected.length; i++) {
					try {
						receiver.retryMessage(selected[i]);
					} catch (Throwable e) {
						error(", ", "errors.generic", "Could not resend message with id ["+selected[i]+"]", e);
					}
				}
			}
 
		} catch (Throwable e) {
			error(", ", "errors.generic", "Error occurred performing action [" + action + "]", e);
		}
	}

	private void deleteMessage(IMessageBrowser mb, String messageId, PlatformTransactionManager transactionManager) throws Throwable {
		TransactionStatus txStatus = null;
		try {
			txStatus = transactionManager.getTransaction(TXNEW);
			mb.deleteMessage(messageId);
		} catch (Throwable e) {
			txStatus.setRollbackOnly();
			error(", ", "errors.generic", "error occured deleting message", e);
			throw e;
		} finally { 
			transactionManager.commit(txStatus);
		}
	}

//	private void resendMessage(Adapter adapter, ReceiverBase receiver, IMessageBrowser mb, String messageId, PlatformTransactionManager transactionManager) throws Throwable {
//		TransactionStatus txStatus = null;
//		try {
//			txStatus = transactionManager.getTransaction(TXNEW);
//			Object msg = mb.getMessage(messageId);
//			if (msg==null) {
//				error(", ", "errors.generic", "did not retrieve message", null);
//				txStatus.setRollbackOnly();
//			} else {
//				if (adapter.getRunState()!=RunStateEnum.STARTED) {
//					error(", ", "errors.generic", "message not processed: adapter not open", null);
//					txStatus.setRollbackOnly();
//				} else {
//					receiver.processRawMessage(receiver.getListener(),msg,null,-1);
//				}
//			}
//		} catch (Throwable e) {
//			txStatus.setRollbackOnly();
//			error(", ", "errors.generic", "error occured resending message", e);
//		} finally { 
//			transactionManager.commit(txStatus);
//		}
//	}


}
