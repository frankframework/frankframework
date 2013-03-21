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
 * $Log: SendJmsMessage.java,v $
 * Revision 1.6  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.4  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringTagger;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;


/**
 * Send a message with JMS.
 * 
 * @author  Johan Verrips
 * @version $Id$
 * @see nl.nn.adapterframework.configuration.Configuration
 */
public final class SendJmsMessage extends ActionBase {
	public static final String version = "$RCSfile: SendJmsMessage.java,v $ $Revision: 1.6 $ $Date: 2011-11-30 13:51:46 $";
	
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
	
	    DynaActionForm sendJmsMessageForm = getPersistentForm(mapping, form, request);
	
	    Cookie[] cookies = request.getCookies();
	
	    if (null != cookies) {
	        for (int i = 0; i < cookies.length; i++) {
	            Cookie aCookie = cookies[i];
	
	            if (aCookie.getName().equals(AppConstants.getInstance().getProperty("WEB_JMSCOOKIE_NAME"))) {
	                StringTagger cs = new StringTagger(aCookie.getValue());
	
	                log.debug("restoring values from cookie: " + cs.toString());
	                try {
	                    sendJmsMessageForm.set("jmsRealm", cs.Value("jmsRealm"));
	                    sendJmsMessageForm.set("destinationName", cs.Value("destinationName"));
	                    sendJmsMessageForm.set("destinationType", cs.Value("destinationType"));
	                } catch (Exception e) {
	                    log.warn("could not restore Cookie value's");
	                }
	            }
	        }
	    }
	
	    List jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
	    if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
	    sendJmsMessageForm.set("jmsRealms", jmsRealms);
	    
	    // Forward control to the specified success URI
	    log.debug("forward to success");
	    return (mapping.findForward("success"));
	
	}
}
