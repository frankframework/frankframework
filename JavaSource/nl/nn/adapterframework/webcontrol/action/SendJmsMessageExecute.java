package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.util.StringTagger;
import org.apache.struts.action.*;
import org.apache.struts.upload.FormFile;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Executes the sending of a message with JMS
 * <p>$Id: SendJmsMessageExecute.java,v 1.2 2004-02-04 10:02:09 a1909356#db2admin Exp $</p>
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 */

public final class SendJmsMessageExecute extends ActionBase {
	public static final String version="$Id: SendJmsMessageExecute.java,v 1.2 2004-02-04 10:02:09 a1909356#db2admin Exp $";
	
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
        qms.sendMessage("testmessage",form_message);
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
	    ArrayList jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
	    if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
	    form.set("jmsRealms", jmsRealms);

	}
}
