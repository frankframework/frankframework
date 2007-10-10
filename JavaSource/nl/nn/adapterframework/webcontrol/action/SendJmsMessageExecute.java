/*
 * $Log: SendJmsMessageExecute.java,v $
 * Revision 1.4.6.1  2007-10-10 14:30:38  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.5  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.4  2004/03/26 10:42:58  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/23 16:58:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * now uses unique ID for sending test-messages
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;
import org.apache.struts.upload.FormFile;


/**
 * Executes the sending of a message with JMS
 * @version Id
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 */

public final class SendJmsMessageExecute extends ActionBase {
	public static final String version = "$RCSfile: SendJmsMessageExecute.java,v $ $Revision: 1.4.6.1 $ $Date: 2007-10-10 14:30:38 $";
	
	public ActionForward execute(
	    ActionMapping mapping,
	    ActionForm form,
	    HttpServletRequest request,
	    HttpServletResponse response)
	    throws IOException, ServletException {
	
	    // Initialize action
	    initAction(request);
	    if (null == config) {
	        return (mapping.findForward("noconfig"));
	    }
	    
	
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
	
	
		if ((form_file!=null) && (form_file.getFileSize()>0)){
			form_message=new String(form_file.getFileData());
			log.debug("Upload of file ["+form_file.getFileName()+"] ContentType["+form_file.getContentType()+"]");
	
		}
	    // initiate MessageSender
	    JmsSender qms = new JmsSender();
	    qms.setName("SendJmsMessageAction");
	    qms.setJmsRealm(form_jmsRealm);
	    qms.setDestinationName(form_destinationName);
	    qms.setPersistent(form_persistent);
	    qms.setDestinationType(form_destinationType);
	    if ((form_replyToName!=null) && (form_replyToName.length()>0))
		    qms.setReplyToName(form_replyToName);
	
	    try {
		    qms.open();
	        qms.sendMessage("testmsg_"+Misc.createUUID(),form_message);
	    } catch (SenderException e) {
	        log.error(e);
	        errors.add(
	            "",
	            new ActionError(
	                "errors.generic",
	                "error occured sending message:" + XmlUtils.encodeChars(e.getMessage())));
	    }
	    try {
		    qms.close();
	    } catch (Exception e) {
	        log.error(e);
	        errors.add(
	            "",
	            new ActionError(
	                "errors.generic",
	                "error occured on closing connection:" +  XmlUtils.encodeChars(e.getMessage())));
	    }
	
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
		    StoreFormData(sendJmsMessageForm);
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
	public void StoreFormData(DynaActionForm form){
	    List jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
	    if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
	    form.set("jmsRealms", jmsRealms);

	}
}
