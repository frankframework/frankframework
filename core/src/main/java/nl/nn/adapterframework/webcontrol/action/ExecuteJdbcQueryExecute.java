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
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StringTagger;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.DynaActionForm;

/**
 * @author m168309 
 */
public final class ExecuteJdbcQueryExecute extends ActionBase {
	public static final String DB2XML_XSLT="xml/xsl/dbxml2csv.xslt";

	public ExecuteJdbcQueryExecute() {
		setWriteToSecLog(true);
		setWriteSecLogMessage(true);
		addSecLogParamName("jmsRealm");
		addSecLogParamName("queryType");
	}
	
	public String getResult(String form_jmsRealm, String form_queryType, String form_resultType, String form_query) {
		DirectQuerySender qs;
		String result = "";
		
		try {
			qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);
				qs.setQueryType(form_queryType);
				qs.setBlobSmartGet(true);
				qs.configure(true);
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

		
		
		DynaActionForm executeJdbcQueryExecuteForm = (DynaActionForm) form;
		String form_jmsRealm = (String) executeJdbcQueryExecuteForm.get("jmsRealm");
		String form_queryType = (String) executeJdbcQueryExecuteForm.get("queryType");
		String form_resultType = (String) executeJdbcQueryExecuteForm.get("resultType");
		String form_query = (String) executeJdbcQueryExecuteForm.get("query");

		XmlBuilder xbRoot = new XmlBuilder("manageDatabaseREQ");
		
		XmlBuilder xSql = new XmlBuilder("sql");
		xSql.addAttribute("jmsRealm", form_jmsRealm);
		xbRoot.addSubElement(xSql);

		XmlBuilder xQType = new XmlBuilder("type");
		xQType.setValue(form_query.split(" ")[0]);
		xSql.addSubElement(xQType);
		
		XmlBuilder xQuery = new XmlBuilder("query");
		xQuery.setValue(form_query);
		xSql.addSubElement(xQuery);
		
		System.out.println("xbRoot: " + xbRoot.toXML());
		JavaListener listener = JavaListener.getListener("ManageDatabase");
		String resultA = "";
		try {
			resultA = listener.processRequest(session.getId(), xbRoot.toXML(), new HashMap());
			System.out.println("Result (JL pre-transform): " + resultA);
			if (form_resultType.equalsIgnoreCase("csv")) {
				URL url= ClassUtils.getResourceURL(this,DB2XML_XSLT);
				if (url!=null) {
					Transformer t = XmlUtils.createTransformer(url);
					resultA = XmlUtils.transformXml(t,resultA);
					System.out.println("Result from listener: " + resultA);
				}
			}
		} catch (ListenerException e1) {
			error("error occured on creating or closing connection",e1);
		} catch (Throwable e) {
			error("error occured on executing jdbc query", e);
		}
		
		
		
		

		DirectQuerySender qs;
		String result = "";
		
		try {
			qs = (DirectQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(DirectQuerySender.class);
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(form_jmsRealm);
				qs.setQueryType(form_queryType);
				qs.setBlobSmartGet(true);
				qs.configure(true);
				qs.open();
				result = qs.sendMessage("dummy", form_query);
				System.out.println(result);
				if ("csv".equalsIgnoreCase(form_resultType)) {
					URL url= ClassUtils.getResourceURL(this,DB2XML_XSLT);
					if (url!=null) {
						Transformer t = XmlUtils.createTransformer(url);
						System.out.println("Result (QS 1/2): " + result);
						result = XmlUtils.transformXml(t,result);
						System.out.println("Result (QS 2/2): " + result);
					}
				}
			} catch (Throwable t) {
				error("error occured on executing jdbc query", t);
			} finally {
				qs.close();
			}
		} catch (Exception e) {
			error("error occured on creating or closing connection", e);
		}
		
		System.out.println("RESULT FROM LISTENER.PROCESSREQUEST() AND SENDER.SENDMESSAGE() IS EQUAL: "
				+ result.equals(resultA));
		
//		String result = "";
//		if ("csv".equalsIgnoreCase(form_resultType)) {
//			URL url= ClassUtils.getResourceURL(this,DB2XML_XSLT);
//			if (url!=null) {
//				try {
//					Transformer t = XmlUtils.createTransformer(url);
//					result = XmlUtils.transformXml(t,result);
//				} catch (Throwable t) {
//					error("error occured on executing jdbc query",t);
//				}
//				
//				System.out.println(result);
//			}
//		}

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
