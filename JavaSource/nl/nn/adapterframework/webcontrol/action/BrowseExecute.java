/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.8.2.2  2008-06-24 08:26:28  europe\L190409
 * sync from HEAD
 *
 * Revision 1.10  2008/06/24 08:00:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * prepare for export of messages in zipfile
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

import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;

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
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.8.2.2 $ $Date: 2008-06-24 08:26:28 $";
    
    protected static final TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    
	protected void performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId, String selected[], HttpServletResponse response) {
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

			if ("export selected".equalsIgnoreCase(action)) {
				OutputStream out = response.getOutputStream();
				response.setContentType("application/x-zip-compressed");
				response.setHeader("Content-Disposition","attachment; filename=\"IbisConsoleDump-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+"\"");
				ZipOutputStream zipOutputStream = new ZipOutputStream(out);
				IListener listener = receiver.getListener();
				for(int i=0; i<selected.length; i++) {
					String id=selected[i];
					String filename="msg_"+id.replace(':','-');
					zipOutputStream.putNextEntry(new ZipEntry(filename));
					try {
						Object rawmsg = mb.browseMessage(id);
						String msg=null;
						if (listener!=null) {
							msg = listener.getStringFromRawMessage(rawmsg,null);
						} else {
							msg=(String)rawmsg;
						}
						if (StringUtils.isEmpty(msg)) {
							msg="<no message found>";
						}
						String encoding=Misc.DEFAULT_INPUT_STREAM_ENCODING;
						if (msg.startsWith("<?xml")) {
							int lastpos=msg.indexOf("?>");
							if (lastpos>0) {
								String prefix=msg.substring(6,lastpos);
								int encodingStartPos=prefix.indexOf("encoding=\"");
								if (encodingStartPos>0) {
									int encodingEndPos=prefix.indexOf('"',encodingStartPos+10);
									if (encodingEndPos>0) {
										encoding=prefix.substring(encodingStartPos+10,encodingEndPos-1);
									}
								}
							}
						}
						zipOutputStream.write(msg.getBytes(encoding));
					} catch (Throwable e) {
						error(", ", "errors.generic", "Could not export message with id ["+selected[i]+"]", e);
					}
					zipOutputStream.closeEntry();
				}
				zipOutputStream.close();
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
