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
 * $Log: TestIfsaService.java,v $
 * Revision 1.4  2011-11-30 13:51:45  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.2  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.1  2005/04/14 08:07:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduction of TestIfsaService-functionality
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

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringTagger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;


/**
 * Send a test message to an IFSA Service.
 * 
 * @author Gerrit van Brakel / Johan Verrips
 * @version $Id$
 */
public final class TestIfsaService extends ActionBase {
	public static final String version = "$RCSfile: TestIfsaService.java,v $ $Revision: 1.4 $ $Date: 2011-11-30 13:51:45 $";

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
	
	    List protocols=new ArrayList();
	    protocols.add("RR");
		protocols.add("FF");
		sendIfsaMessageForm.set("messageProtocols", protocols);
	    
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
