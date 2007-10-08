/*
 * $Log: ExecuteJdbcQuery.java,v $
 * Revision 1.2  2007-10-08 13:41:35  europe\L190409
 * changed ArrayList to List where possible
 *
 * Revision 1.1  2005/10/27 12:20:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * execQuery functionality in IbisConsole
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

import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Executes a query.
 * 
 * @author m168309
 * @version Id 
 */

public final class ExecuteJdbcQuery extends ActionBase {
	public static final String version = "$RCSfile: ExecuteJdbcQuery.java,v $ $Revision: 1.2 $ $Date: 2007-10-08 13:41:35 $";

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

		IniDynaActionForm executeJdbcQueryForm = (IniDynaActionForm) form;
		Cookie[] cookies = request.getCookies();

		if (null != cookies) {
			for (int i = 0; i < cookies.length; i++) {
				Cookie aCookie = cookies[i];

				if (aCookie
					.getName()
					.equals(
						AppConstants.getInstance().getString(
							"WEB_EXECJDBCCOOKIE_NAME",
							"WEB_EXECJDBCCOOKIE"))) {
					StringTagger cs = new StringTagger(aCookie.getValue());

					log.debug("restoring values from cookie: " + cs.toString());
					try {
						executeJdbcQueryForm.set(
							"jmsRealm",
							cs.Value("jmsRealm"));
						executeJdbcQueryForm.set(
							"queryType",
							cs.Value("queryType"));
						executeJdbcQueryForm.set(
							"resultType",
							cs.Value("resultType"));
						executeJdbcQueryForm.set("query", cs.Value("query"));
					} catch (Exception e) {
						log.warn("could not restore Cookie value's", e);
					}
				}

			}
		}

		List jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
			jmsRealms.add("no realms defined");
		executeJdbcQueryForm.set("jmsRealms", jmsRealms);

		List queryTypes = new ArrayList();
		queryTypes.add("select");
		queryTypes.add("other");
		executeJdbcQueryForm.set("queryTypes", queryTypes);

		List resultTypes = new ArrayList();
		resultTypes.add("csv");
		resultTypes.add("xml");
		executeJdbcQueryForm.set("resultTypes", resultTypes);

		// Forward control to the specified success URI
		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
}
