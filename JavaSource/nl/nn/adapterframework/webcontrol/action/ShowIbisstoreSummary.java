/*
 * $Log: ShowIbisstoreSummary.java,v $
 * Revision 1.1  2009-04-16 08:58:04  L190409
 * first version of ShowIbisstoreSummary
 *
 */

package nl.nn.adapterframework.webcontrol.action;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Show counts per day of each slot in the ibisstore.
 * 
 * @author  Gerrit van Brakel
 * @version Id 
 * @since	4.9.7
 */

public class ShowIbisstoreSummary extends ActionBase {
	public static final String version = "$RCSfile: ShowIbisstoreSummary.java,v $ $Revision: 1.1 $ $Date: 2009-04-16 08:58:04 $";

	public static final String SHOWIBISSTORECOOKIE="ShowIbisstoreSummaryCookieName";

	public void addPropertiesToXmlBuilder(XmlBuilder container, Properties props, String setName) {
		Enumeration enum = props.keys();
		XmlBuilder propertySet = new XmlBuilder("propertySet");
		propertySet.addAttribute("name", setName);
		container.addSubElement(propertySet);

		while (enum.hasMoreElements()) {
			String propName = (String) enum.nextElement();
			XmlBuilder property = new XmlBuilder("property");
			property.addAttribute("name", XmlUtils.encodeCdataString(propName));
			property.setCdataValue(XmlUtils.encodeCdataString(props.getProperty(propName)));
			propertySet.addSubElement(property);
		}

	}
	

	public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		IniDynaActionForm showIbisstoreSummaryForm = (IniDynaActionForm) form;
		// Initialize action
		initAction(request);

		String jmsRealm = (String) showIbisstoreSummaryForm.get("jmsRealm");
		String cookieName=AppConstants.getInstance().getString(SHOWIBISSTORECOOKIE,SHOWIBISSTORECOOKIE);

		if (StringUtils.isEmpty(jmsRealm)) {
			// get jmsRealm value from cookie
			Cookie[] cookies = request.getCookies();
			if (null != cookies) {
				for (int i = 0; i < cookies.length; i++) {
					Cookie aCookie = cookies[i];

					if (aCookie.getName().equals(cookieName)) {
						jmsRealm = aCookie.getValue();
						log.debug("jmsRealm from cookie [" + jmsRealm +"]");
					}
				}
			}
		}

		List jmsRealms = JmsRealmFactory.getInstance().getRegisteredRealmNamesAsList();
		if (jmsRealms.size() == 0) {
			jmsRealms.add("no realms defined");
		} else {
			if (StringUtils.isEmpty(jmsRealm)) {
				jmsRealm=(String)jmsRealms.get(0);
			}
		}
		showIbisstoreSummaryForm.set("jmsRealms", jmsRealms);

		if (StringUtils.isNotEmpty(jmsRealm)) {

			String formQuery=
				"select type, slotid, to_char(MESSAGEDATE,'YYYY-MM-DD') msgdate, count(*) msgcount" 
				+ " from ibisstore group by slotid, type, to_char(MESSAGEDATE,'YYYY-MM-DD')"
				+ " order by type, slotid, to_char(MESSAGEDATE,'YYYY-MM-DD')"
				;

			DirectQuerySender qs;
			String result = "";

			try {
				qs = new DirectQuerySender() {
//					protected String getResult(ResultSet resultset) throws JdbcException, SQLException, IOException {
//						XmlBuilder result = new XmlBuilder("result");
//						String previousType=null;
//						XmlBuilder typeXml=null;
//						String previousSlot=null;
//						XmlBuilder slotXml=null;
//						int typeslotcount=0;
//						int typedatecount=0;
//						int typemsgcount=0;
//						int slotdatecount=0;
//						int slotmsgcount=0;
//						while (resultset.next()) {
//							String type = resultset.getString("type");
//							String slotid = resultset.getString("slotid");
//							String date =  resultset.getString("msgdate");
//							int count =    resultset.getInt("msgcount");
//							
//						
//							if (!type.equals(previousType)) {
//								if (typeXml!=null) {
//									typeXml.addAttribute("slotcount",typeslotcount);
//									typeXml.addAttribute("datecount",typedatecount);
//									typeXml.addAttribute("msgcount",typemsgcount);
//									typeslotcount=0;
//									typedatecount=0;
//									typemsgcount=0;
//									previousSlot=null;
//								}
//								typeXml=new XmlBuilder("type");
//								typeXml.addAttribute("id",type);
//								result.addSubElement(typeXml);
//								previousType=type;
//							}
//							if (!slotid.equals(previousSlot)) {
//								if (slotXml!=null) {
//									slotXml.addAttribute("datelinecount",slotdatecount);
//									slotXml.addAttribute("msgcount",slotmsgcount);
//									slotdatecount=0;
//									slotmsgcount=0;
//								}
//								slotXml=new XmlBuilder("slot");
//								slotXml.addAttribute("id",slotid);
//								typeXml.addSubElement(slotXml);
//								previousSlot=slotid;
//								typeslotcount++;
//							}
//							typemsgcount+=count;
//							typedatecount++;
//							slotmsgcount+=count;
//							slotdatecount++;
//							
//							XmlBuilder dateXml=new XmlBuilder("date");
//							dateXml.addAttribute("id",date);
//							dateXml.addAttribute("count",count);
//							slotXml.addSubElement(dateXml);
//						}
//						if (typeXml!=null) {
//							typeXml.addAttribute("slotcount",typeslotcount);
//							typeXml.addAttribute("datecount",typedatecount);
//							typeXml.addAttribute("msgcount",typemsgcount);
//						}
//						if (slotXml!=null) {
//							slotXml.addAttribute("datelinecount",slotdatecount);
//							slotXml.addAttribute("msgcount",slotmsgcount);
//							slotdatecount=0;
//							slotmsgcount=0;
//						}
//						return result.toXML();
//					}
				};
				try {
					qs.setName("QuerySender");
					qs.setJmsRealm(jmsRealm);
					qs.setQueryType("select");
					qs.setBlobSmartGet(true);
					qs.configure();
					qs.open();
					result = qs.sendMessage("dummy", formQuery);
//					URL url= ClassUtils.getResourceURL(this,ExecuteJdbcQueryExecute.DB2XML_XSLT);
//					if (url!=null) {
//						Transformer t = XmlUtils.createTransformer(url);
//						result = XmlUtils.transformXml(t,result);
//					}
				} catch (Throwable t) {
					error("error occured on executing jdbc query",t);
				} finally {
					qs.close();
				}
			} catch (Exception e) {
				error("error occured on creating or closing connection",e);
			}
//			log.debug("result ["+result+"]");
			showIbisstoreSummaryForm.set("result", result);
			request.setAttribute("result", result);

		}


		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (new ActionForward(mapping.getInput()));
		}

		//Successfull: store cookie
		String cookieValue = jmsRealm;
		Cookie cookie =	new Cookie(cookieName, cookieValue);
		cookie.setMaxAge(Integer.MAX_VALUE);
		log.debug("Store cookie for "
				+ request.getServletPath()
				+ " cookieName[" + cookieName + "] "
				+ " cookieValue[" + cookieValue + "]");
		try {
			response.addCookie(cookie);
		} catch (Throwable t) {
			log.warn("unable to add cookie to request. cookie value [" + cookie.getValue() + "]",t);
		}

		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
}
