/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.14  2009-01-02 10:27:55  m168309
 * fixed bug
 *
 * Revision 1.13  2008/12/10 17:05:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed bug in export selected messages; now a valid zip file is returned
 *
 * Revision 1.12  2008/07/24 12:39:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.11  2008/06/26 12:51:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix export selected
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
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.ITransactionalStorage;
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
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.14 $ $Date: 2009-01-02 10:27:55 $";
    
    protected static final TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    
	protected boolean performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId, String selected[], HttpServletResponse response) {
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
				response.setHeader("Content-Disposition","attachment; filename=\"messages-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+".zip\"");
				ZipOutputStream zipOutputStream = new ZipOutputStream(out);
				IListener listener = null;
				if (receiver!=null) {
					listener = receiver.getListener();
				}
				for(int i=0; i<selected.length; i++) {
					try {
						Object rawmsg = mb.browseMessage(selected[i]);
						String msg=null;
						String id=selected[i];
						HashMap context = new HashMap();
						if (listener!=null) {
							msg = listener.getStringFromRawMessage(rawmsg,context);
							id= listener.getIdFromRawMessage(rawmsg,context);
						} else {
							msg=(String)rawmsg;
						}
						if (StringUtils.isEmpty(msg)) {
							msg="<no message found>";
						} else {
							log.debug("id ["+id+"] msg ["+msg+"]");
						}
						if (StringUtils.isEmpty(id)) {
							id=""+i; 
						}
						String filename="msg_"+id.replace(':','-')+".txt";
						ZipEntry zipEntry=new ZipEntry(filename);
						//zipEntry.setTime();
						zipOutputStream.putNextEntry(zipEntry);
						String encoding=Misc.DEFAULT_INPUT_STREAM_ENCODING;
						if (msg.startsWith("<?xml")) {
							int lastpos=msg.indexOf("?>");
							if (lastpos>0) {
								String prefix=msg.substring(6,lastpos);
								int encodingStartPos=prefix.indexOf("encoding=\"");
								if (encodingStartPos>0) {
									int encodingEndPos=prefix.indexOf('"',encodingStartPos+10);
									if (encodingEndPos>0) {
										encoding=prefix.substring(encodingStartPos+10,encodingEndPos);
										log.debug("parsed encoding ["+encoding+"] from prefix ["+prefix+"]");
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
				return true;
			}

 
		} catch (Throwable e) {
			error(", ", "errors.generic", "Error occurred performing action [" + action + "]", e);
		}
		return false;
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

}
