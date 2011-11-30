/*
 * $Log: BrowseJdbcTable.java,v $
 * Revision 1.7  2011-11-30 13:51:46  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.5  2011/08/22 08:37:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed incomplete cookies
 *
 * Revision 1.4  2008/11/12 12:34:24  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed error in cookies
 *
 * Revision 1.3  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.2  2007/05/21 12:25:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed version string
 *
 * Revision 1.1  2007/05/21 12:24:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added browseJdbcTable functions
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

public class BrowseJdbcTable extends ActionBase {
	public static final String version = "$RCSfile: BrowseJdbcTable.java,v $ $Revision: 1.7 $ $Date: 2011-11-30 13:51:46 $";

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

		if (form == null) {
			log.debug(
				" Creating new browseJdbcTableForm bean under key "
					+ mapping.getAttribute());

			IniDynaActionForm browseJdbcTableForm = new IniDynaActionForm();

			if ("request".equals(mapping.getScope())) {
				request.setAttribute(mapping.getAttribute(), form);
			} else {
				session.setAttribute(mapping.getAttribute(), form);
			}
		}

		IniDynaActionForm browseJdbcTableForm = (IniDynaActionForm) form;
		Cookie[] cookies = request.getCookies();

		if (null != cookies) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie aCookie = cookies[i];

				if (aCookie
					.getName()
					.equals(
						AppConstants.getInstance().getProperty(
							"WEB_JDBCBROWSECOOKIE_NAME"))) {
					StringTagger cs = new StringTagger(aCookie.getValue());

					log.debug("restoring values from cookie: " + cs.toString());
					try {
						browseJdbcTableForm.set(
							"jmsRealm",
							cs.Value("jmsRealm"));
						browseJdbcTableForm.set(
							"tableName",
							cs.Value("tableName"));
						browseJdbcTableForm.set(
							"where",
							cs.Value("where"));
						browseJdbcTableForm.set(
							"order",
						cs.Value("order"));
						browseJdbcTableForm.set(
							"numberOfRowsOnly",
							new Boolean(cs.Value("numberOfRowsOnly")));
						browseJdbcTableForm.set(
							"rownumMin",
							new Integer(cs.Value("rownumMin")));
						browseJdbcTableForm.set(
							"rownumMax",
							new Integer(cs.Value("rownumMax")));
					} catch (Exception e) {
						log.warn("could not restore Cookie value's", e);
					}
				}

			}
		}

		List jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
			jmsRealms.add("no realms defined");
		browseJdbcTableForm.set("jmsRealms", jmsRealms);

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}

}
