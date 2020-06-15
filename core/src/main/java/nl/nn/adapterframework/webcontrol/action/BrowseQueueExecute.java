/*
   Copyright 2013, 2016 Nationale-Nederlanden

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
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsBrowser;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

/**
 * @author Johan Verrips
 */
public class BrowseQueueExecute extends ActionBase {

	public ActionForward executeSub(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws IOException, ServletException {

		// Initialize action
		initAction(request);

		// Was this transaction cancelled?
		// -------------------------------
		if (isCancelled(request)) {
			log.debug("browseQueue was cancelled");
			removeFormBean(mapping, request);
			return (mapping.findForward("cancel"));
		}

		// Retrieve form content
		// ---------------------
		IniDynaActionForm browseQueueForm = (IniDynaActionForm) form;
		String form_jmsRealm = (String) browseQueueForm.get("jmsRealm");
		String form_destinationName =
			(String) browseQueueForm.get("destinationName");
		String form_destinationType =
			(String) browseQueueForm.get("destinationType");
		boolean form_numberOfMessagesOnly = false;
		boolean form_showPayload=false;
		if (browseQueueForm.get("numberOfMessagesOnly") != null)
			form_numberOfMessagesOnly =
				((Boolean) browseQueueForm.get("numberOfMessagesOnly"))
					.booleanValue();
			if (browseQueueForm.get("showPayload") != null)
				form_showPayload =
					((Boolean) browseQueueForm.get("showPayload"))
						.booleanValue();

		// initiate MessageSender
		JmsBrowser<javax.jms.Message> jmsBrowser = new JmsBrowser<>();
		jmsBrowser.setName("BrowseQueueAction");
		jmsBrowser.setJmsRealm(form_jmsRealm);
		jmsBrowser.setDestinationName(form_destinationName);
		jmsBrowser.setDestinationType(form_destinationType);
		IMessageBrowsingIterator it=null;
		try {
			it = jmsBrowser.getIterator();
			List messages = new ArrayList();
			while (it.hasNext()) {
				messages.add(it.next());
			}
			log.debug("Browser returned " + messages.size() + " messages");
			browseQueueForm.set("numberOfMessages", Integer.toString(messages.size()));
			if (!form_numberOfMessagesOnly) {
/*				
				try {
					for (int i = 0; i < messages.size(); i++) {
						Message msg = (Message) messages.get(i);
						if (msg instanceof TextMessage) {
							TextMessage tm = (TextMessage) msg;
							if (log.isDebugEnabled())
								log.debug("Found message " + tm.getText());
						}
					}
				} catch (JMSException je) {
					log.error(je);
					errors.add(
						"",
						new ActionError(
							"errors.generic",
							"error occured browsing messages:" + je.getMessage()));
				}
*/				
				browseQueueForm.set("messages", messages);
			} else
				browseQueueForm.set("messages", new ArrayList());
		} catch (ListenerException e) {
			error("Error occured browsing messages", e);
		} finally {
			try {
				if (it!=null) {
					it.close();
				}
			} catch (ListenerException e1) {
				log.error(e1);
			}
		}

		// Report any errors we have discovered back to the original form
		if (!errors.isEmpty()) {
			StoreFormData(browseQueueForm);
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
		cookieValue += " "; //separator          
		cookieValue += "showPayload=\"" + form_showPayload + "\"";
		log.debug(
			"*** value :  "
				+ AppConstants.getInstance().getString(
					"WEB_QBROWSECOOKIE_NAME",
					"WEB_QBROWSECOOKIE"));
		Cookie sendJmsCookie =
			new Cookie(
				AppConstants.getInstance().getString(
					"WEB_QBROWSECOOKIE_NAME",
					"WEB_QBROWSECOOKIE"),
				cookieValue);
		sendJmsCookie.setMaxAge(Integer.MAX_VALUE);
		sendJmsCookie.setHttpOnly(true);
		sendJmsCookie.setSecure(true);
		log.debug(
			"Store cookie for "
				+ request.getServletPath()
				+ " cookieName["
				+ sendJmsCookie.getName()
				+ "] "
				+ " cookieValue["
				+ new StringTagger(cookieValue).toString()
				+ "]");
		try {
			response.addCookie(sendJmsCookie);
		} catch (Throwable e) {
			log.warn(
				"unable to add cookie to request. cookie value ["
					+ sendJmsCookie.getValue()
					+ "]");
		}

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
	public void StoreFormData(IniDynaActionForm form) {
		List jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
			jmsRealms.add("no realms defined");
		form.set("jmsRealms", jmsRealms);

	}

}
