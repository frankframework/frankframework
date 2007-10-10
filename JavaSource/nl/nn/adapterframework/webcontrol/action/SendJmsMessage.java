/*
 * $Log: SendJmsMessage.java,v $
 * Revision 1.3.6.1  2007-10-10 14:30:38  europe\L190409
 * synchronize with HEAD (4.8-alpha1)
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
 * @version Id
 * @see nl.nn.adapterframework.configuration.Configuration
 */
public final class SendJmsMessage extends ActionBase {
	public static final String version = "$RCSfile: SendJmsMessage.java,v $ $Revision: 1.3.6.1 $ $Date: 2007-10-10 14:30:38 $";
	
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
