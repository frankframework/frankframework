/*
 * $Log: BrowseJdbcTableExecute.java,v $
 * Revision 1.12  2011-08-22 08:37:05  m168309
 * fixed incomplete cookies
 *
 * Revision 1.11  2011/03/16 16:38:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * databasetype now defined in DbmsSupportFactory
 *
 * Revision 1.10  2009/10/19 14:01:23  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * includeFieldDefinition=true
 *
 * Revision 1.9  2009/09/25 12:28:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added WHERE facility
 *
 * Revision 1.8  2008/11/12 12:34:24  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * fixed error in cookies
 *
 * Revision 1.7  2008/11/05 12:22:06  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * impose limits to prevent memory exception
 *
 * Revision 1.6  2008/10/20 13:02:44  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * also show not compressed blobs and not serialized blobs
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
	public static final String version = "$RCSfile: BrowseJdbcTableExecute.java,v $ $Revision: 1.12 $ $Date: 2011-08-22 08:37:05 $";
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
