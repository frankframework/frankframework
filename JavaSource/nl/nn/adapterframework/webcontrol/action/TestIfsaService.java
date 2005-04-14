/*
 * $Log: TestIfsaService.java,v $
 * Revision 1.1  2005-04-14 08:07:57  L190409
 * introduction of TestIfsaService-functionality
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringTagger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Send a test message to an IFSA Service.
 * 
 * @version Id
 * @author Gerrit van Brakel / Johan Verrips
 */

public final class TestIfsaService extends ActionBase {
	public static final String version="$Id: TestIfsaService.java,v 1.1 2005-04-14 08:07:57 L190409 Exp $";

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
	
	    DynaActionForm sendIfsaMessageForm = getPersistentForm(mapping, form, request);
	
	    Cookie[] cookies = request.getCookies();
	
	    if (null != cookies) {
	        for (int i = 0; i < cookies.length; i++) {
	            Cookie aCookie = cookies[i];
	
	            if (aCookie.getName().equals(AppConstants.getInstance().getProperty("WEB_IFSACOOKIE_NAME"))) {
	                StringTagger cs = new StringTagger(aCookie.getValue());
	
	                log.debug("restoring values from cookie: " + cs.toString());
	                try {
	                    sendIfsaMessageForm.set(
	                        "applicationId",
	                        cs.Value("applicationId"));
	                    sendIfsaMessageForm.set(
		                    "serviceId",
		                    cs.Value("serviceId"));
	                    sendIfsaMessageForm.set(
		                    "messageProtocol",
		                    cs.Value("messageProtocol"));
	                } catch (Exception e) {
	                    log.warn("could not restore Cookie value's");
	                }
	            }
	
	        }
	    }
	
	    ArrayList protocols=new ArrayList();
	    protocols.add("RR");
		protocols.add("FF");
		sendIfsaMessageForm.set("messageProtocols", protocols);
	    
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
