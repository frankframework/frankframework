package nl.nn.adapterframework.webcontrol.action;

import nl.nn.adapterframework.jms.JmsRealmFactory;
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
 * Send a message with JMS
 * @author  Johan Verrips
 * @see nl.nn.adapterframework.configuration.Configuration
 */

public final class SendJmsMessage extends ActionBase {
	public static final String version="$Id: SendJmsMessage.java,v 1.1 2004-02-04 08:36:16 a1909356#db2admin Exp $";
	


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
                    sendJmsMessageForm.set(
                        "jmsRealm",
                        cs.Value("jmsRealm"));
                    sendJmsMessageForm.set(
	                    "destinationName",
	                    cs.Value("destinationName"));
                    sendJmsMessageForm.set(
	                    "destinationType",
	                    cs.Value("destinationType"));
                } catch (Exception e) {
                    log.warn("could not restore Cookie value's");
                }
            }

        }
    }

    ArrayList jmsRealms=JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
    if (jmsRealms.size()==0) jmsRealms.add("no realms defined");
    sendJmsMessageForm.set("jmsRealms", jmsRealms);
    
    // Forward control to the specified success URI
    log.debug("forward to success");
    return (mapping.findForward("success"));

}
}
