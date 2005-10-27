/*
 * $Log: ExecuteJdbcQueryExecute.java,v $
 * Revision 1.1  2005-10-27 12:20:03  europe\L190409
 * execQuery functionality in IbisConsole
 *
 */

package nl.nn.adapterframework.webcontrol.action;

import org.apache.struts.action.*;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

/**
 * @version Id 
 * @author m168309
 */

public final class ExecuteJdbcQueryExecute extends ActionBase {
	public static final String version = "$RCSfile: ExecuteJdbcQueryExecute.java,v $ $Revision: 1.1 $ $Date: 2005-10-27 12:20:03 $";
	public static final String DB2XML_XSLT="xml/xsl/dbxml2csv.xslt";

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

		DynaActionForm executeJdbcQueryExecuteForm = (DynaActionForm) form;
		String form_jmsRealm = (String) executeJdbcQueryExecuteForm.get("jmsRealm");
		String form_queryType = (String) executeJdbcQueryExecuteForm.get("queryType");
		String form_resultType = (String) executeJdbcQueryExecuteForm.get("resultType");
		String form_query = (String) executeJdbcQueryExecuteForm.get("query");

		DirectQuerySender qs;
		String result = "";

		try {
			qs = new DirectQuerySender();
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);
				qs.setQueryType(form_queryType);
				qs.configure();
				qs.open();
				result = qs.sendMessage("dummy", form_query);
				if ("csv".equalsIgnoreCase(form_resultType)) {
					URL url= ClassUtils.getResourceURL(this,DB2XML_XSLT);
					if (url!=null) {
						Transformer t = XmlUtils.createTransformer(url);
						result = XmlUtils.transformXml(t,result);
					}
				}
			} catch (Throwable t) {
				log.error(t);
				errors.add(
					"",
					new ActionError(
						"errors.generic",
						"error occured on executing jdbc query: "
							+ XmlUtils.encodeChars(t.getMessage())));
			} finally {
				qs.close();
			}
		} catch (Exception e) {
			log.error(e);
			errors.add(
				"",
				new ActionError(
					"errors.generic",
					"error occured on creating or closing connection: "
						+ XmlUtils.encodeChars(e.getMessage())));
		}

		StoreFormData(form_query, result, executeJdbcQueryExecuteForm);

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (new ActionForward(mapping.getInput()));
		}

		//Successfull: store cookie
		String cookieValue = "";
		cookieValue += "jmsRealm=\"" + form_jmsRealm + "\"";
		cookieValue += " "; //separator
		cookieValue += "queryType=\"" + form_queryType + "\"";
		cookieValue += " "; //separator
		cookieValue += "resultType=\"" + form_resultType + "\"";
		cookieValue += " "; //separator
		cookieValue += "query=\"" + form_query + "\"";
		Cookie execJdbcCookie =
			new Cookie(
				AppConstants.getInstance().getProperty(
					"WEB_EXECJDBCCOOKIE_NAME"),
				cookieValue);
		execJdbcCookie.setMaxAge(Integer.MAX_VALUE);
		log.debug(
			"Store cookie for "
				+ request.getServletPath()
				+ " cookieName["
				+ AppConstants.getInstance().getProperty(
					"WEB_EXECJDBCCOOKIE_NAME")
				+ "] "
				+ " cookieValue["
				+ new StringTagger(cookieValue).toString()
				+ "]");
		try {
			response.addCookie(execJdbcCookie);
		} catch (Throwable e) {
			log.warn(
				"unable to add cookie to request. cookie value ["
					+ execJdbcCookie.getValue()
					+ "]");
		}

		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
	public void StoreFormData(
		String query,
		String result,
		DynaActionForm executeJdbcQueryExecuteForm) {
		ArrayList jmsRealms =
			JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
			jmsRealms.add("no realms defined");
		executeJdbcQueryExecuteForm.set("jmsRealms", jmsRealms);
		ArrayList queryTypes = new ArrayList();
		queryTypes.add("select");
		queryTypes.add("other");
		executeJdbcQueryExecuteForm.set("queryTypes", queryTypes);

		ArrayList resultTypes = new ArrayList();
		resultTypes.add("csv");
		resultTypes.add("xml");
		executeJdbcQueryExecuteForm.set("resultTypes", resultTypes);
		if (null != query)
			executeJdbcQueryExecuteForm.set("query", query);
		if (null != result) {
			executeJdbcQueryExecuteForm.set("result", result);

		}
	}
}
