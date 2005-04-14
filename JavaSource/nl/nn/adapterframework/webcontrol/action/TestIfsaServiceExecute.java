/*
 * $Log: TestIfsaServiceExecute.java,v $
 * Revision 1.2  2005-04-14 09:12:32  L190409
 * exchanged message and result
 *
 * Revision 1.1  2005/04/14 08:07:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of TestIfsaService-functionality
 *
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.util.StringTagger;
import org.apache.struts.action.*;
import org.apache.struts.upload.FormFile;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * Executes the sending of a test message to an IFSA Service.
 * 
 * @version Id
 * @author  Gerrit van Brakel / Johan Verrips
 */

public final class TestIfsaServiceExecute extends ActionBase {
	public static final String version="$Id: TestIfsaServiceExecute.java,v 1.2 2005-04-14 09:12:32 L190409 Exp $";
	
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
	        log.debug("testIfsaService was cancelled");
	        removeFormBean(mapping, request);
	        return (mapping.findForward("success"));
	    }
	
	    // Retrieve form content
	    // ---------------------
	    DynaActionForm sendIfsaMessageForm = (DynaActionForm) form;
	    String form_applicationId =(String) sendIfsaMessageForm.get("applicationId");
	    String form_serviceId = (String) sendIfsaMessageForm.get("serviceId");
	    String form_messageProtocol = (String) sendIfsaMessageForm.get("messageProtocol");
	    String form_message = (String) sendIfsaMessageForm.get("message");
	    FormFile form_file=(FormFile) sendIfsaMessageForm.get("file");
	
	
		if ((form_file!=null) && (form_file.getFileSize()>0)){
			form_message=new String(form_file.getFileData());
			log.debug("Upload of file ["+form_file.getFileName()+"] ContentType["+form_file.getContentType()+"]");
	
		}
	
		IfsaRequesterSender sender;
		String result="";
	    try {
			// initiate MessageSender
			sender = new IfsaRequesterSender();
			try {
				sender.setName("testIfsaServiceAction");
				sender.setApplicationId(form_applicationId);
				sender.setServiceId(form_serviceId);
				sender.setMessageProtocol(form_messageProtocol);
	
		    	sender.configure();
			    sender.open();
		        result = sender.sendMessage("testmsg_"+Misc.createUUID(),form_message);
		    } catch (Throwable t) {
		        log.error(t);
		        errors.add(
		            "",
		            new ActionError(
		                "errors.generic",
		                "error occured sending message:" + XmlUtils.encodeChars(t.getMessage())));
		    }
		    sender.close();
	    } catch (Exception e) {
	        log.error(e);
	        errors.add(
	            "",
	            new ActionError(
	                "errors.generic",
	                "error occured on creating object or closing connection:" +  XmlUtils.encodeChars(e.getMessage())));
	    }
	
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
		    StoreFormData(form_message, result, sendIfsaMessageForm);
		    saveErrors(request, errors);
	        return (new ActionForward(mapping.getInput()));
	    }
	
		//Successfull: store cookie
	          String cookieValue = "";
	          cookieValue += "applicationId=\"" + form_applicationId + "\"";
	          cookieValue += " "; //separator
	          cookieValue += "serviceId=\"" + form_serviceId + "\"";
	          cookieValue += " "; //separator          
	          cookieValue += "messageProtocol=\"" + form_messageProtocol + "\"";
	          Cookie sendIfsaCookie = new Cookie(AppConstants.getInstance().getProperty("WEB_IFSACOOKIE_NAME"), cookieValue);
	          sendIfsaCookie.setMaxAge(Integer.MAX_VALUE);
	          log.debug("Store cookie for " + request.getServletPath()+
	          " cookieName[" + AppConstants.getInstance().getProperty("WEB_IFSACOOKIE_NAME")+"] "+
	          " cookieValue[" + new StringTagger(cookieValue).toString()+"]");
	          try {
		          response.addCookie(sendIfsaCookie);
	          } catch (Throwable e) {
		          log.warn("unable to add cookie to request. cookie value ["+sendIfsaCookie.getValue()+"]");
	          }
	    
	   
		
	    
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	}
	
	public void StoreFormData(String message, String result, DynaActionForm form){
		if (null!=message) form.set("message", message);
		if (null != result) {
			form.set("result", result);
		}
	}
	
}
