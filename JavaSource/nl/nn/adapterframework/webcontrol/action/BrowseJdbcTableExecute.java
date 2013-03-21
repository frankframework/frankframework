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
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;

import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.dbms.DbmsSupportFactory;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class BrowseJdbcTableExecute extends ActionBase {
	public static final String version = "$RCSfile: BrowseJdbcTableExecute.java,v $ $Revision: 1.14 $ $Date: 2011-11-30 13:51:46 $";
	public static final String DB2XML_XSLT = "xml/xsl/BrowseJdbcTableExecute.xsl";

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

		// Was this transaction cancelled?
		// -------------------------------
		if (isCancelled(request)) {
			log.debug("browseJdbcTable was cancelled");
			removeFormBean(mapping, request);
			return (mapping.findForward("cancel"));
		}

		// Retrieve form content
		// ---------------------
		IniDynaActionForm browseJdbcTableForm = (IniDynaActionForm) form;
		String form_jmsRealm = (String) browseJdbcTableForm.get("jmsRealm");
		String form_tableName = (String) browseJdbcTableForm.get("tableName");
		String form_where = (String) browseJdbcTableForm.get("where");
		boolean form_numberOfRowsOnly = false;
		String form_order = (String) browseJdbcTableForm.get("order");
		if (browseJdbcTableForm.get("numberOfRowsOnly") != null)
			form_numberOfRowsOnly =
				((Boolean) browseJdbcTableForm.get("numberOfRowsOnly"))
					.booleanValue();
		int form_rownumMin = ((Integer) browseJdbcTableForm.get("rownumMin")).intValue();
		int form_rownumMax = ((Integer) browseJdbcTableForm.get("rownumMax")).intValue();

		if (!form_numberOfRowsOnly) {
			if (form_rownumMin < 0) {
				form_rownumMin = 0;
			}
			if (form_rownumMax < 0) {
				form_rownumMax = 0;
			}
			if (form_rownumMin == 0 && form_rownumMax == 0) {
				form_rownumMin = 1;
				form_rownumMax = 100;
			}
			if (errors.isEmpty()) {
				if (form_rownumMax < form_rownumMin) {
					error("errors.generic","Rownum max must be greater than or equal to Rownum min",null);
				}
			}
			if (errors.isEmpty()) {
				if (form_rownumMax - form_rownumMin >= 100) {
					error("errors.generic","Difference between Rownum max and Rownum min must be less than hundred",null);
				}
			}
		}

		if (errors.isEmpty()) {
			DirectQuerySender qs;
			String result = "";
			String query = null;
			try {
				qs = new DirectQuerySender();
				try {
					qs.setName("QuerySender");
					qs.setJmsRealm(form_jmsRealm);

					if (form_numberOfRowsOnly || qs.getDatabaseType() == DbmsSupportFactory.DBMS_ORACLE) {
						qs.setQueryType("select");
						qs.setBlobSmartGet(true);
						qs.setIncludeFieldDefinition(true);
						qs.configure();
						qs.open();
						query = "SELECT * FROM " + form_tableName + " WHERE ROWNUM=0";
						result = qs.sendMessage("dummy", query);
						String browseJdbcTableExecuteREQ =
							"<browseJdbcTableExecuteREQ>"
								+ "<tableName>"
								+ form_tableName
								+ "</tableName>"
								+ "<where>"
								+ XmlUtils.encodeChars(form_where)
								+ "</where>"
								+ "<numberOfRowsOnly>"
								+ form_numberOfRowsOnly
								+ "</numberOfRowsOnly>"
								+ "<order>"
								+ form_order
								+ "</order>"
								+ "<rownumMin>"
								+ form_rownumMin
								+ "</rownumMin>"
								+ "<rownumMax>"
								+ form_rownumMax
								+ "</rownumMax>"
								+ result
								+ "</browseJdbcTableExecuteREQ>";
						URL url = ClassUtils.getResourceURL(this, DB2XML_XSLT);
						if (url != null) {
							Transformer t = XmlUtils.createTransformer(url);
							query = XmlUtils.transformXml(t, browseJdbcTableExecuteREQ);
						}
						result = qs.sendMessage("dummy", query);
					} else {
						error("errors.generic","This function only supports oracle databases",null);
					}
				} catch (Throwable t) {
					error("errors.generic","error occured on executing jdbc query [" + query + "]",t);
				} finally {
					qs.close();
				}
			} catch (Exception e) {
				error("errors.generic","error occured on creating or closing connection",e);
			}
			String resultEnvelope =
				"<resultEnvelope>"
					+ "<request "
					+ "tableName=\""
					+ form_tableName
					+ "\">"
					+ XmlUtils.encodeChars(query)
					+ "</request>"
					+ result
					+ "</resultEnvelope>";

			request.setAttribute("DB2Xml", resultEnvelope);
		}

		// Report any errors we have discovered back to the original form
		if (!errors.isEmpty()) {
			StoreFormData(browseJdbcTableForm);
			saveErrors(request, errors);
			return (new ActionForward(mapping.getInput()));
		}

		//Successfull: store cookie
		String cookieValue = "";
		cookieValue += "jmsRealm=\"" + form_jmsRealm + "\"";
		cookieValue += " "; //separator
		cookieValue += "tableName=\"" + form_tableName + "\"";
		cookieValue += " "; //separator          
		cookieValue += "where=\"" + form_where + "\"";
		cookieValue += " "; //separator          
		cookieValue += "order=\"" + form_order + "\"";
		cookieValue += " "; //separator          
		cookieValue += "numberOfRowsOnly=\"" + form_numberOfRowsOnly + "\"";
		cookieValue += " "; //separator          
		cookieValue += "rownumMin=\"" + form_rownumMin + "\"";
		cookieValue += " "; //separator          
		cookieValue += "rownumMax=\"" + form_rownumMax + "\"";
		Cookie sendJdbcBrowseCookie =
			new Cookie(
				AppConstants.getInstance().getProperty(
					"WEB_JDBCBROWSECOOKIE_NAME"),
				cookieValue);
		sendJdbcBrowseCookie.setMaxAge(Integer.MAX_VALUE);
		log.debug(
			"Store cookie for "
				+ request.getServletPath()
				+ " cookieName["
				+ AppConstants.getInstance().getProperty(
					"WEB_JDBCBROWSECOOKIE_NAME")
				+ "] "
				+ " cookieValue["
				+ new StringTagger(cookieValue).toString()
				+ "]");
		try {
			response.addCookie(sendJdbcBrowseCookie);
		} catch (Throwable e) {
			log.warn(
				"unable to add cookie to request. cookie value ["
					+ sendJdbcBrowseCookie.getValue()
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
