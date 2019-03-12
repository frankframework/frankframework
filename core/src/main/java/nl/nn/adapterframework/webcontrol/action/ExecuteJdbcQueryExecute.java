/*
   Copyright 2013, 2016, 2019 Nationale-Nederlanden

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * @author m168309 
 */
public final class ExecuteJdbcQueryExecute extends ActionBase {
	public static final String DB2XML_XSLT="xml/xsl/dbxml2csv.xslt";
	public static final String XML2CSV_XSLT="xml/xsl/xmlresult2csv.xsl";

	public ExecuteJdbcQueryExecute() {
		setWriteToSecLog(true);
		setWriteSecLogMessage(true);
		addSecLogParamName("datasourceName");
		addSecLogParamName("expectResultSet");
	}
	
	public String getResult(String form_datasourceName, String form_expectResultSet, String form_resultType, String form_query) {
		XmlBuilder xbRoot = new XmlBuilder("manageDatabaseREQ");
		
		XmlBuilder xSql = new XmlBuilder("sql");
		xSql.addAttribute("datasourceName", form_datasourceName.split("] ")[1]);
		xSql.addAttribute("expectResultSet", form_expectResultSet);
		xbRoot.addSubElement(xSql);

		XmlBuilder xQType = new XmlBuilder("type");
		xQType.setValue(form_query.split(" ")[0]);
		xSql.addSubElement(xQType);
		
		XmlBuilder xQuery = new XmlBuilder("query");
		xQuery.setValue(form_query);
		xSql.addSubElement(xQuery);
		
		// Send XML to ManageDatabase adapter
		JavaListener listener = JavaListener.getListener("ManageDatabase");
		String result = "";
		try {
			result = listener.processRequest(session.getId(), xbRoot.toXML(), new HashMap());
			
			if (form_resultType.equalsIgnoreCase("csv")) {
				URL url= ClassUtils.getResourceURL(this,XML2CSV_XSLT);
				if (url!=null) {
					Transformer t = XmlUtils.createTransformer(url);
					result = XmlUtils.transformXml(t,result);
				}
			}
		} catch (ListenerException e1) {
			error("error occured on creating or closing connection",e1);
		} catch (Throwable e) {
			error("error occured on executing jdbc query", e);
		}
		
		return result;
	}

	public ActionForward executeSub(
		ActionMapping mapping,
		ActionForm form,
		HttpServletRequest request,
		HttpServletResponse response)
		throws IOException, ServletException {

		// Initialize action
		initAction(request);

		// Transfer form results into XML
		DynaActionForm executeJdbcQueryExecuteForm = (DynaActionForm) form;
		String form_datasourceName = (String) executeJdbcQueryExecuteForm.get("datasourceName");
		String form_expectResultSet = (String) executeJdbcQueryExecuteForm.get("expectResultSet");
		String form_resultType = (String) executeJdbcQueryExecuteForm.get("resultType");
		String form_query = (String) executeJdbcQueryExecuteForm.get("query");
		
		String result = getResult(form_datasourceName, form_expectResultSet, form_resultType, form_query);
		if(result.isEmpty()) {
			result += "[Query \""+form_query+"\" was successfully executed.]";
		}
		
		// Store form data on page
		StoreFormData(form_query, result, executeJdbcQueryExecuteForm);

		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (new ActionForward(mapping.getInput()));
		}

		// Successful: store cookie
		String cookieValue = "";
		cookieValue += "datasourceName=\"" + form_datasourceName + "\"";
		cookieValue += " "; //separator
		cookieValue += "expectResult=\"" + form_expectResultSet + "\"";
		cookieValue += " "; //separator
		cookieValue += "resultType=\"" + form_resultType + "\"";
		cookieValue += " "; //separator
		//TODO: fix workaround to avoid http error 500 on WAS (line separators in query cookie)
		String fq = Misc.replace(form_query, System.getProperty("line.separator"), " ");
		cookieValue += "query=\"" + fq + "\"";
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
		List<String> realmNames = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		List<String> datasourceNames = new ArrayList<String>();
		for(String s : realmNames) {
			datasourceNames.add("["+s+"] " + JmsRealmFactory.getInstance().getJmsRealm(s).getDatasourceName());
		}
		if (datasourceNames.size() == 0)
			datasourceNames.add("no data sources defined");
		executeJdbcQueryExecuteForm.set("datasourceNames", datasourceNames);
		
		List<String> expectResultOptions = new ArrayList<String>();
		expectResultOptions.add("auto");
		expectResultOptions.add("yes");
		expectResultOptions.add("no");
		executeJdbcQueryExecuteForm.set("expectResultOptions", expectResultOptions);

		List<String> resultTypes = new ArrayList<String>();
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
