/*
 * $Log: BrowseQueue.java,v $
 * Revision 1.5  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.3  2011/03/16 16:38:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic change
 *
 * Revision 1.2  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.1  2004/06/16 12:25:52  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version of Queue browsing functionality
 *
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
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;



/**
 * Send the form to get input for displaying messages from a queue.
 * 
 * @author Johan Verrips
 * @since 4.1.1
 * @version Id
 */
public class BrowseQueue extends ActionBase {
	public static final String version = "$RCSfile: BrowseQueue.java,v $ $Revision: 1.5 $ $Date: 2011-11-30 13:51:46 $";
	

public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

	// Initialize action
	initAction(request);
	if (null == config) {
		return (mapping.findForward("noconfig"));
	}


	if (form == null) {
		log.debug(" Creating new browseQueueForm bean under key [" + mapping.getAttribute()+"]");

		IniDynaActionForm browseQueueForm = new IniDynaActionForm();

		if ("request".equals(mapping.getScope())) {
			request.setAttribute(mapping.getAttribute(), form);
		} else {
			session.setAttribute(mapping.getAttribute(), form);
		}
	} 

	IniDynaActionForm browseQueueForm = (IniDynaActionForm) form;
	Cookie[] cookies = request.getCookies();

	if (null != cookies) {
		for (int i = 0; i < cookies.length; i++) {
			Cookie aCookie = cookies[i];

			if (aCookie.getName().equals(AppConstants.getInstance().getString("WEB_QBROWSECOOKIE_NAME", "WEB_QBROWSECOOKIE"))) {
				StringTagger cs = new StringTagger(aCookie.getValue());

				log.debug("restoring values from cookie: " + cs.toString());
				try {
					browseQueueForm.set("jmsRealm",	       cs.Value("jmsRealm"));
					browseQueueForm.set("destinationName", cs.Value("destinationName"));
					browseQueueForm.set("destinationType", cs.Value("destinationType"));
					browseQueueForm.set("numberOfMessagesOnly", new Boolean(cs.Value("numberOfMessagesOnly")));
					browseQueueForm.set("showPayload", new Boolean(cs.Value("showPayload")));
				} catch (Exception e) {
					log.warn("could not restore Cookie value's",e);
				}
			}

		}
	}

	List jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
	if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
	browseQueueForm.set("jmsRealms", jmsRealms);

	// Forward control to the specified success URI
	log.debug("forward to success");
	return (mapping.findForward("success"));

}


}

