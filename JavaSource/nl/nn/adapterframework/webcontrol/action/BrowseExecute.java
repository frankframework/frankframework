/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.4  2007-10-10 07:31:29  europe\L190409
 * transactions via JtaUtil instead of Adapter
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

import javax.transaction.UserTransaction;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.RunStateEnum;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionError;

/**
 * Extension to transactionalstorage browser, that enables delete and repost.
 * 
 * @version Id
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class BrowseExecute extends Browse {
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.4 $ $Date: 2007-10-10 07:31:29 $";

	protected void performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId) {
		if ("deletemessage".equalsIgnoreCase(action)) {
			if (StringUtils.isNotEmpty(messageId)) {
				try {
					// TODO: Redo tx management (where exactly do we get the txManager from?)
					UserTransaction utx = JtaUtil.getUserTransaction();
					utx.begin();
					mb.deleteMessage(messageId);
					utx.commit();
				} catch (Throwable e) {
					log.error(e);
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
					// TODO: Redo tx management (where exactly do we get the txManager from?)
					UserTransaction utx = JtaUtil.getUserTransaction();
					utx.begin();
					Object msg = mb.getMessage(messageId);
					if (msg==null) {
						errors.add("",
							new ActionError(
								"errors.generic",
								"did not retrieve message" ));
						utx.rollback();
					} else {
						if (adapter.getRunState()!=RunStateEnum.STARTED) {
							errors.add("",
								new ActionError(
									"errors.generic",
									"message not processed: adapter not open" ));
							utx.rollback();
						} else {
							receiver.processRawMessage(receiver.getListener(),msg,null,-1);
						}
					}
				} catch (Throwable e) {
					log.error(e);
					errors.add("",
						new ActionError(
							"errors.generic",
							"error occured sending message:" + e.getMessage()));
				}
			}
		} 
	}


}
