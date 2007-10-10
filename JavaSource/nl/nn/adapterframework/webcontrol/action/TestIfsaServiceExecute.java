/*
 * $Log: TestIfsaServiceExecute.java,v $
 * Revision 1.5.6.1  2007-10-10 14:30:37  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.6  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.5  2005/06/28 09:03:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * close in finally-clause
 *
 * Revision 1.4  2005/04/14 09:54:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * saved results
 *
 * Revision 1.3  2005/04/14 09:28:12  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * saved message protocols to form
 *
 * Revision 1.2  2005/04/14 09:12:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * exchanged message and result
 *
 * Revision 1.1  2005/04/14 08:07:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of TestIfsaService-functionality
 *
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.extensions.ifsa.IfsaRequesterSender;
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
 * Executes the sending of a test message to an IFSA Service.
 * 
 * @author  Gerrit van Brakel / Johan Verrips
 * @since   4.3
 * @version Id
 */
public final class TestIfsaServiceExecute extends ActionBase {
	public static final String version = "$RCSfile: TestIfsaServiceExecute.java,v $ $Revision: 1.5.6.1 $ $Date: 2007-10-10 14:30:37 $";
	
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
		    } finally {
				sender.close();
		    }
	    } catch (Exception e) {
	        log.error(e);
	        errors.add(
	            "",
	            new ActionError(
	                "errors.generic",
	                "error occured on creating object or closing connection:" +  XmlUtils.encodeChars(e.getMessage())));
	    }
		StoreFormData(form_message, result, sendIfsaMessageForm);
	
	    // Report any errors we have discovered back to the original form
	    if (!errors.isEmpty()) {
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
		List protocols=new ArrayList();
		protocols.add("RR");
		protocols.add("FF");
		form.set("messageProtocols", protocols);
	}
	
}
