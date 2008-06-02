/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.3.8.2  2008-06-02 15:35:43  europe\L190409
 * added resend-all processing
 *
 * Revision 1.3.8.1  2008/06/02 15:10:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * ErrorStoreClient: fixed bug in presentation of errors, error messages are now XML-escaped.
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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

import javax.servlet.ServletException;
import javax.transaction.UserTransaction;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.XmlUtils;

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
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.3.8.2 $ $Date: 2008-06-02 15:35:43 $";

	protected void performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId) {
		if ("deletemessage".equalsIgnoreCase(action)) {
			if (StringUtils.isNotEmpty(messageId)) {
				try {
					UserTransaction utx = adapter.getUserTransaction();
					utx.begin();
					mb.deleteMessage(messageId);
					utx.commit();
				} catch (Throwable e) {
					log.error(e);
					errors.add("",
						new ActionError(
							"errors.generic",
							"error occured deleting message:" +XmlUtils.encodeChars(e.getMessage())));
				}
			}
		} 

		if ("resendmessage".equalsIgnoreCase(action)) {
			resendMessage(adapter,receiver,mb,messageId);
		}

		if ("resendAll".equalsIgnoreCase(action)) {
			try {
				IMessageBrowsingIterator mbi=mb.getIterator();
				int messageCount;
				for (messageCount=0; mbi.hasNext();) {
					Object iterItem = mbi.next();
					String id = mb.getId(iterItem);
					messageCount++;
					log.debug("Resend ErrorLog record [" + messageCount	+ "] with id ["	+ id + "]");
					resendMessage(adapter,receiver,mb,id);
				}
			} catch (Throwable e) {
				log.error(e);
				errors.add("",
					new ActionError(
						"errors.generic",
						"error occured iterating over ErrorLog:" + XmlUtils.encodeChars(e.getMessage())));
			}
		}
			
	}

	private void resendMessage(Adapter adapter, ReceiverBase receiver, IMessageBrowser mb, String messageId) {
		if (StringUtils.isNotEmpty(messageId)) {
			try {
				UserTransaction utx = adapter.getUserTransaction();
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
						"error occured sending message:" + XmlUtils.encodeChars(e.getMessage())));
			}
		}
	} 

}
