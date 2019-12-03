/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;


/**
 * Executes the sending of a message with JMS.
 * <p>
 * For setting the JMS correlationId a processing instruction with the name <code>ibiscontext</code> and key <code>tcid</code> has to be used<br/><br/>
 * example:<br/><code><pre>
 * &lt;?ibiscontext tcid=1234567890?&gt;
 * &lt;message&gt;This is a Message&lt;/message&gt;
 * </pre></code><br/>
 * 
 * @author  Johan Verrips
 */
public final class SendJmsMessageExecute extends ActionBase {

	public SendJmsMessageExecute() {
		setWriteToSecLog(true);
		setWriteSecLogMessage(true);
		addSecLogParamName("jmsRealm");
		addSecLogParamName("destinationName");
	}

	public ActionForward executeSub(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    
	
	    // Was this transaction cancelled?
	    // -------------------------------
	    if (isCancelled(request)) {
	        log.debug("sendJmsMessage was cancelled");
	        removeFormBean(mapping, request);
	        return (mapping.findForward("success"));
	    }
	
	    // Retrieve form content
	    // ---------------------
	    DynaActionForm sendJmsMessageForm = (DynaActionForm) form;
	    String form_jmsRealm=(String) sendJmsMessageForm.get("jmsRealm");
	    String form_destinationName = (String) sendJmsMessageForm.get("destinationName");
	    String form_destinationType = (String) sendJmsMessageForm.get("destinationType");
	    boolean form_persistent=false;
	    if (null!=sendJmsMessageForm.get("persistent")) {
	    	form_persistent = ((Boolean) sendJmsMessageForm.get("persistent")).booleanValue();
	    }
	    String form_message = (String) sendJmsMessageForm.get("message");
	    FormFile form_file=(FormFile) sendJmsMessageForm.get("file");
	    String form_replyToName=(String) sendJmsMessageForm.get("replyToName");

		// if upload is choosen, it prevails over the message
		if ((form_file != null) && (form_file.getFileSize() > 0)) {
			log.debug("Upload of file ["+form_file.getFileName()+"] ContentType["+form_file.getContentType()+"]");
			if (FileUtils.extensionEqualsIgnoreCase(form_file.getFileName(),"zip")) {
				ZipInputStream archive = new ZipInputStream(new ByteArrayInputStream(form_file.getFileData()));
				for (ZipEntry entry=archive.getNextEntry(); entry!=null; entry=archive.getNextEntry()) {
					String name = entry.getName();
					int size = (int)entry.getSize();
					if (size>0) {
						byte[] b=new byte[size];
						int rb=0;
						int chunk=0;
						while (((int)size - rb) > 0) {
							chunk=archive.read(b,rb,(int)size - rb);
							if (chunk==-1) {
								break;
							}
							rb+=chunk;
						}
						String currentMessage = XmlUtils.readXml(b,0,rb,request.getCharacterEncoding(),false);
						// initiate MessageSender
						JmsSender qms = new JmsSender();
						qms.setName("SendJmsMessageAction");
						qms.setJmsRealm(form_jmsRealm);
						qms.setDestinationName(form_destinationName);
						qms.setPersistent(form_persistent);
						qms.setDestinationType(form_destinationType);
						if ((form_replyToName!=null) && (form_replyToName.length()>0))
							qms.setReplyToName(form_replyToName);
		
						processMessage(qms, name+"_" + Misc.createSimpleUUID(), currentMessage);
					}
					archive.closeEntry();
				}
				archive.close();
				form_message = null;
			} else {
				form_message = XmlUtils.readXml(form_file.getFileData(),request.getCharacterEncoding(),false);
			}
		} else {
			form_message=new String(form_message.getBytes(),Misc.DEFAULT_INPUT_STREAM_ENCODING);
		}
			
		if(form_message != null && form_message.length() > 0) {
			// initiate MessageSender
			JmsSender qms = new JmsSender();
			qms.setName("SendJmsMessageAction");
			qms.setJmsRealm(form_jmsRealm);
			qms.setDestinationName(form_destinationName);
			qms.setPersistent(form_persistent);
			qms.setDestinationType(form_destinationType);
			if ((form_replyToName!=null) && (form_replyToName.length()>0))
				qms.setReplyToName(form_replyToName);
	
			processMessage(qms, "testmsg_"+Misc.createUUID(), form_message);
		}

		StoreFormData(sendJmsMessageForm);
	
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
		    saveErrors(request, errors);
	        return (new ActionForward(mapping.getInput()));
	    }
	
		//Successfull: store cookie
	          String cookieValue = "";
	          cookieValue += "jmsRealm=\"" + form_jmsRealm + "\"";
	          cookieValue += " "; //separator
	          cookieValue += "destinationName=\"" + form_destinationName + "\"";
	          cookieValue += " "; //separator          
	          cookieValue += "destinationType=\"" + form_destinationType + "\"";
	          Cookie sendJmsCookie = new Cookie(AppConstants.getInstance().getProperty("WEB_JMSCOOKIE_NAME"), cookieValue);
	          sendJmsCookie.setMaxAge(Integer.MAX_VALUE);
	          log.debug("Store cookie for " + request.getServletPath()+
	          " cookieName[" + AppConstants.getInstance().getProperty("WEB_JMSCOOKIE_NAME")+"] "+
	          " cookieValue[" + new StringTagger(cookieValue).toString()+"]");
	          try {
		          response.addCookie(sendJmsCookie);
	          } catch (Throwable e) {
		          log.warn("unable to add cookie to request. cookie value ["+sendJmsCookie.getValue()+"]");
	          }
	    
	   
		
	    
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}

	private void processMessage(JmsSender qms, String messageId, String message) {
		//PipeLineSession pls=new PipeLineSession();
		Map ibisContexts = XmlUtils.getIbisContext(message);
		String technicalCorrelationId = messageId;
		if (log.isDebugEnabled()) {
			if (ibisContexts!=null) {
				String contextDump = "ibisContext:";
				for (Iterator it = ibisContexts.keySet().iterator(); it.hasNext();) {
					String key = (String)it.next();
					String value = (String)ibisContexts.get(key);
					if (log.isDebugEnabled()) {
						contextDump = contextDump + "\n " + key + "=[" + value + "]";
					}
					if (key.equals("tcid")) {
						technicalCorrelationId = value;
					}
				}
				log.debug(contextDump);
			}
		}

		try {
			qms.open();
			qms.sendMessage(technicalCorrelationId,message);
		} catch (SenderException e) {
			error("error occured sending message",e);
		} catch (TimeOutException e) {
			error("error occured sending message",e);
		}
		try {
			qms.close();
		} catch (Exception e) {
			error("error occured on closing connection",e);
		}
	}
	
	public void StoreFormData(DynaActionForm form){
	    List jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
	    if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
	    form.set("jmsRealms", jmsRealms);

	}
}
