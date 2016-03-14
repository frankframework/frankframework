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
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;

import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DB2XMLWriter;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.log4j.Logger;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class BrowseJdbcTableExecute extends ActionBase {
	public static final String DB2XML_XSLT = "xml/xsl/BrowseJdbcTableExecute.xsl";
	private static Logger log = LogUtil.getLogger(BrowseJdbcTableExecute.class);
	private static final String permissionRules = AppConstants.getInstance().getResolvedProperty("browseJdbcTable.permission.rules");

	public ActionForward executeSub(
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
			if (!readAllowed(permissionRules, request, form_tableName)) {
				error("errors.generic","Access to table ("+form_tableName+") not allowed",null);
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

					//if (form_numberOfRowsOnly || qs.getDatabaseType() == DbmsSupportFactory.DBMS_ORACLE) {
						qs.setQueryType("select");
						qs.setBlobSmartGet(true);
						qs.setIncludeFieldDefinition(true);
						qs.configure(true);
						qs.open();

						ResultSet rs = qs.getConnection().getMetaData().getColumns(null, null, form_tableName, null);
						if (!rs.isBeforeFirst()) {
							rs = qs.getConnection().getMetaData().getColumns(null, null, form_tableName.toUpperCase(), null);
						}
						
						String fielddefinition = "<fielddefinition>";
						while(rs.next()) {
							String field = "<field name=\""
									+ rs.getString(4)
									+ "\" type=\""
									+ DB2XMLWriter.getFieldType(rs.getInt(5))
									+ "\" size=\""
									+ rs.getInt(7)
									+ "\"/>";
							fielddefinition = fielddefinition + field;
						}
						fielddefinition = fielddefinition + "</fielddefinition>";
						
						String browseJdbcTableExecuteREQ =
							"<browseJdbcTableExecuteREQ>"
								+ "<dbmsName>"
								+ qs.getDbmsSupport().getDbmsName()
								+ "</dbmsName>"
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
								+ fielddefinition
								+ "<maxColumnSize>1000</maxColumnSize>"
								+ "</browseJdbcTableExecuteREQ>";
						URL url = ClassUtils.getResourceURL(this, DB2XML_XSLT);
						if (url != null) {
							Transformer t = XmlUtils.createTransformer(url);
							query = XmlUtils.transformXml(t, browseJdbcTableExecuteREQ);
						}
						result = qs.sendMessage("dummy", query);
					//} else {
						//error("errors.generic","This function only supports oracle databases",null);
					//}
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

	public static boolean readAllowed(String rules, HttpServletRequest request, String tableName) throws IOException {
		tableName = tableName.toLowerCase();
		List<String> rulesList = Arrays.asList(rules.split("\\|"));
		for (String rule: rulesList) {
			List<String> parts = Arrays.asList(rule.trim().split("\\s+"));
			if (parts.size() != 3) {
				log.debug("invalid rule '" + rule + "' contains " + parts.size() + " part(s): " + parts);
			} else {
				String tablePattern = parts.get(0).toLowerCase();
				if (tableName != null && tablePattern != null) {
					String role = parts.get(1);
					String type = parts.get(2);
					log.debug("check allow read table '" + tableName + "' with rule table '" + tablePattern + "', role '" + role + "' and type '" + type + "'");
					if ("*".equals(tablePattern) || tableName.equals(tablePattern)) {
						log.debug("table match");
						if ("*".equals(role) || request.isUserInRole(role)) {
							log.debug("role match");
							if ("allow".equals(type)) {
								log.debug("allow");
								return true;
							} else if ("deny".equals(type)) {
								log.debug("deny");
								return false;
							} else {
								log.error("invalid rule type");
							}
						}
					}
				}
			}
		}
		log.debug("deny");
		return false;
	}

}
