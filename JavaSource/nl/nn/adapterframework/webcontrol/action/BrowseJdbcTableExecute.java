/*
 * $Log: BrowseJdbcTableExecute.java,v $
 * Revision 1.3.4.2  2008-06-19 07:11:32  europe\L190409
 * sync from HEAD
 *
 * Revision 1.5  2008/06/18 09:17:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected rownums together with order by (MO)
 *
 * Revision 1.4  2008/05/22 07:34:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use inherited error() method
 *
 * Revision 1.3  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.2  2007/05/24 09:54:47  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made cloudscape compatible
 *
 * Revision 1.1  2007/05/21 12:24:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added browseJdbcTable functions
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcFacade;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

public class BrowseJdbcTableExecute extends ActionBase {
	public static final String version = "$RCSfile: BrowseJdbcTableExecute.java,v $ $Revision: 1.3.4.2 $ $Date: 2008-06-19 07:11:32 $";

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
		boolean form_numberOfRowsOnly = false;
		String form_order = (String) browseJdbcTableForm.get("order");
		if (browseJdbcTableForm.get("numberOfRowsOnly") != null)
			form_numberOfRowsOnly =
				((Boolean) browseJdbcTableForm.get("numberOfRowsOnly"))
					.booleanValue();
		String form_rownumMin = (String) browseJdbcTableForm.get("rownumMin");
		String form_rownumMax = (String) browseJdbcTableForm.get("rownumMax");

		DirectQuerySender qs;
		String result = "";
		String query = null;
		try {
			qs = new DirectQuerySender();
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);

				if (form_numberOfRowsOnly) {
					query = "SELECT COUNT(*) AS rowcount FROM " + form_tableName;
				} else {
					if (qs.getDatabaseType()==JdbcFacade.DATABASE_ORACLE) {					
						if (StringUtils.isNotEmpty(form_order)) {
							query = "SELECT ROW_NUMBER() OVER (ORDER BY "+ form_order +") AS ROWNUMBER, "+form_tableName+ ".* FROM " + form_tableName;
						} else {
							query = "SELECT ROWNUM AS ROWNUMBER, "+form_tableName+ ".* FROM " + form_tableName;
						}
					} else {
						query = "SELECT * FROM " + form_tableName;
					}
					if (qs.getDatabaseType()==JdbcFacade.DATABASE_ORACLE) {					
						if (StringUtils.isNotEmpty(form_rownumMin)
							|| StringUtils.isNotEmpty(form_rownumMax)) {
								
							query = "SELECT * FROM ("+query+") ";
								
							if (StringUtils.isNotEmpty(form_rownumMin)
								&& StringUtils.isNotEmpty(form_rownumMax)) {
								query =
									query
										+ " WHERE ROWNUMBER BETWEEN "
										+ form_rownumMin
										+ " AND "
										+ form_rownumMax;
							} else if (StringUtils.isNotEmpty(form_rownumMin)) {
								query = query + " WHERE ROWNUMBER >= " + form_rownumMin;
							} else if (StringUtils.isNotEmpty(form_rownumMax)) {
								query = query + " WHERE ROWNUMBER <= " + form_rownumMax;
							}
						}		

					}
					
//					String subQuery = null;
//					subQuery =
//						"SELECT ROWNUM AS ROWNUMBER, "
//							+ form_tableName
//							+ ".* FROM "
//							+ form_tableName;
//					query = query + " FROM (" + subQuery + ")";
				}
				qs.setQueryType("select");
				qs.configure();
				qs.open();
				result = qs.sendMessage("dummy", query);
			} catch (Throwable t) {
				error("errors.generic","error occured on executing jdbc query ["+query+"]",t);
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
				+ query
				+ "</request>"
				+ result
				+ "</resultEnvelope>";

		request.setAttribute("DB2Xml", resultEnvelope);

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
