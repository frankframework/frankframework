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
import nl.nn.adapterframework.stream.Message;
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
 * @author  Gerrit van Brakel
 * @since   4.3
 */
public class BrowseExecute extends Browse {
    
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
					msg = listener.extractMessage(rawmsg,context).asString();
				} else {
					msg = Message.asString(rawmsg);
				}
				if (StringUtils.isEmpty(msg)) {
					msg="<no message found>";
				} else {
					msg=Misc.cleanseMessage(msg, mb.getHideRegex(), mb.getHideMethod());
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
					String bulkfilename=bdl.retrieveBulkData(rawmsg,new Message(msg),context);

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
