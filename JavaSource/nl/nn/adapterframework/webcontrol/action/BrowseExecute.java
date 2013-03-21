/*
   Copyright 2013 Nationale-Nederlanden

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
/*
 * $Log: BrowseExecute.java,v $
 * Revision 1.19  2012-06-01 10:52:59  m00f069
 * Created IPipeLineSession (making it easier to write a debugger around it)
 *
 * Revision 1.18  2011/11/30 13:51:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.16  2009/12/23 17:10:23  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified MessageBrowsing interface to reenable and improve export of messages
 *
 * Revision 1.15  2009/08/04 11:46:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * work around IE 6 issue, that prevents exporting messages under HTTPS
 *
 * Revision 1.14  2009/01/02 10:27:55  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
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

import java.io.File;
import java.io.FileInputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IBulkDataListener;
import nl.nn.adapterframework.core.IListener;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.webcontrol.Download;

import org.apache.commons.lang.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Extension to transactionalstorage browser, that enables delete and repost.
 * 
 * @version $Id$
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class BrowseExecute extends Browse {
	public static final String version="$RCSfile: BrowseExecute.java,v $ $Revision: 1.19 $ $Date: 2012-06-01 10:52:59 $";
    
    protected static final TransactionDefinition TXNEW = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    
	protected boolean performAction(Adapter adapter, ReceiverBase receiver, String action, IMessageBrowser mb, String messageId, String selected[], HttpServletRequest request, HttpServletResponse response) {
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

			if ("exportmessage".equalsIgnoreCase(action)) {
				String filename="messages-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+".zip";
				if (StringUtils.isNotEmpty(messageId)) {
					if (Download.redirectForDownload(request, response, "application/x-zip-compressed", filename)) {
						return true;
					}
					ZipOutputStream zipOutputStream = StreamUtil.openZipDownload(response, filename);
					exportMessage(mb,messageId,receiver,zipOutputStream);
					zipOutputStream.close();
					return true;
				}
			}

			if ("export selected".equalsIgnoreCase(action)) {
				String filename="messages-"+AppConstants.getInstance().getProperty("instance.name","")+"-"+Misc.getHostname()+".zip";
				if (Download.redirectForDownload(request, response, "application/x-zip-compressed", filename)) {
					return true;
				}
				ZipOutputStream zipOutputStream = StreamUtil.openZipDownload(response, filename);
				for(int i=0; i<selected.length; i++) {
					exportMessage(mb,selected[i],receiver,zipOutputStream);
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

	private void exportMessage(IMessageBrowser mb, String id, ReceiverBase receiver, ZipOutputStream zipOutputStream) {
		IListener listener = null;
		if (receiver!=null) {
			listener = receiver.getListener();
		}
		try {
			Object rawmsg = mb.browseMessage(id);
			IMessageBrowsingIteratorItem msgcontext=mb.getContext(id);
			try {
				String msg=null;
				String msgId=msgcontext.getId();
				String msgMid=msgcontext.getOriginalId();
				String msgCid=msgcontext.getCorrelationId();
				HashMap context = new HashMap();
				if (listener!=null) {
					msg = listener.getStringFromRawMessage(rawmsg,context);
				} else {
					msg=(String)rawmsg;
				}
				if (StringUtils.isEmpty(msg)) {
					msg="<no message found>";
				}
				if (msgId==null) {
					msgId="";
				}
				if (msgMid==null) {
					msgMid="";
				}
				if (msgCid==null) {
					msgCid="";
				}
				String filename="msg_"+id+"_id["+msgId.replace(':','-')+"]"+"_mid["+msgMid.replace(':','-')+"]"+"_cid["+msgCid.replace(':','-')+"]";
				ZipEntry zipEntry=new ZipEntry(filename+".txt");

				String sentDateString=(String)context.get(IPipeLineSession.tsSentKey);
				if (StringUtils.isNotEmpty(sentDateString)) {
					try {
						Date sentDate = DateUtils.parseToDate(sentDateString, DateUtils.FORMAT_FULL_GENERIC);
						zipEntry.setTime(sentDate.getTime());
					} catch (Throwable e) {
						error(", ", "errors.generic", "Could not set date for message ["+id+"]", e);
					} 
				} else {
					Date insertDate=msgcontext.getInsertDate();
					if (insertDate!=null) {
						zipEntry.setTime(insertDate.getTime());
					}
				}
//				String comment=msgcontext.getCommentString();
//				if (StringUtils.isNotEmpty(comment)) {
//					zipEntry.setComment(comment);
//				}

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
							
				if (listener!=null && listener instanceof IBulkDataListener) {
					IBulkDataListener bdl=(IBulkDataListener)listener;
					String bulkfilename=bdl.retrieveBulkData(rawmsg,msg,context);

					zipOutputStream.closeEntry();
		
					File bulkfile=new File(bulkfilename);

					zipEntry=new ZipEntry(filename+"_"+bulkfile.getName());
					zipEntry.setTime(bulkfile.lastModified());
					zipOutputStream.putNextEntry(zipEntry);
					StreamUtil.copyStream(new FileInputStream(bulkfile),zipOutputStream,32000);
					bulkfile.delete();
				}
				zipOutputStream.closeEntry();
			} finally {
				msgcontext.release();
			}
		} catch (Throwable e) {
			error(", ", "errors.generic", "Could not export message with id ["+id+"]", e);
		}
	}

}
