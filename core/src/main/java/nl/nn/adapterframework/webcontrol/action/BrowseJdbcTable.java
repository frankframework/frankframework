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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CookieUtil;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class BrowseJdbcTable extends ActionBase {

	public ActionForward executeSub(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws IOException, ServletException {

		// Initialize action
		initAction(request);

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

		Cookie cookie = CookieUtil.getCookie(request, AppConstants.getInstance().getProperty("WEB_JDBCBROWSECOOKIE_NAME"));
		if (null != cookie) {
			StringTagger cs = new StringTagger(cookie.getValue());

			log.debug("restoring values from cookie: " + cs.toString());
			try {
				browseJdbcTableForm.set("jmsRealm", cs.Value("jmsRealm"));
				browseJdbcTableForm.set("tableName", cs.Value("tableName"));
				browseJdbcTableForm.set("where", cs.Value("where"));
				browseJdbcTableForm.set("order", cs.Value("order"));
				browseJdbcTableForm.set("numberOfRowsOnly", new Boolean(cs.Value("numberOfRowsOnly")));
				browseJdbcTableForm.set("rownumMin", new Integer(cs.Value("rownumMin")));
				browseJdbcTableForm.set("rownumMax", new Integer(cs.Value("rownumMax")));
			} catch (Exception e) {
				log.warn("could not restore Cookie value's", e);
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
