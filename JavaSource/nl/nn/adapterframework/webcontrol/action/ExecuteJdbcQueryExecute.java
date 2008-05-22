/*
 * $Log: ExecuteJdbcQueryExecute.java,v $
 * Revision 1.3  2008-05-22 07:36:21  europe\L190409
 * use inherited error() method
 *
 * Revision 1.2  2007/10/08 13:41:35  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed ArrayList to List where possible
 *
 * Revision 1.1  2005/10/27 12:20:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * execQuery functionality in IbisConsole
 *
 */
package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
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
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * @author m168309
 * @version Id 
 */
public final class ExecuteJdbcQueryExecute extends ActionBase {
	public static final String version = "$RCSfile: ExecuteJdbcQueryExecute.java,v $ $Revision: 1.3 $ $Date: 2008-05-22 07:36:21 $";
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
				error("error occured on executing jdbc query",t);
			} finally {
				qs.close();
			}
		} catch (Exception e) {
			error("error occured on creating or closing connection",e);
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
		List jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0)
			jmsRealms.add("no realms defined");
		executeJdbcQueryExecuteForm.set("jmsRealms", jmsRealms);
		List queryTypes = new ArrayList();
		queryTypes.add("select");
		queryTypes.add("other");
		executeJdbcQueryExecuteForm.set("queryTypes", queryTypes);

		List resultTypes = new ArrayList();
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
