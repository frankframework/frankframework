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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

/**
 * Show counts per day of each slot in the ibisstore.
 * 
 * @author  Gerrit van Brakel
 * @version $Id$ 
 * @since	4.9.7
 */

public class ShowIbisstoreSummary extends ActionBase {
	public static final String version = "$RCSfile: ShowIbisstoreSummary.java,v $ $Revision: 1.10 $ $Date: 2012-04-04 09:45:52 $";

	public static final String SHOWIBISSTORECOOKIE="ShowIbisstoreSummaryCookieName";
	public static final String SHOWIBISSTOREQUERYKEY="ibisstore.summary.query";

	private class SlotIdRecord {
		
		String adapterName;
		String receiverName;
		String pipeName;
		
		SlotIdRecord(String adapterName, String receiverName, String pipeName) {
			super();
			this.adapterName=adapterName;
			this.receiverName=receiverName;
			this.pipeName=pipeName;
		}
		
	}
	private Map slotmap = new HashMap();

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

		if (null!=config) {
			for(Iterator adapterIt=config.getRegisteredAdapters().iterator(); adapterIt.hasNext();) {
				Adapter adapter = (Adapter)adapterIt.next();
				for(Iterator receiverIt=adapter.getReceiverIterator(); receiverIt.hasNext();) {
					ReceiverBase receiver=(ReceiverBase)receiverIt.next();
					ITransactionalStorage errorStorage=receiver.getErrorStorage();
					if (errorStorage!=null) {
						String slotId=errorStorage.getSlotId();
						if (StringUtils.isNotEmpty(slotId)) {
							SlotIdRecord sir=new SlotIdRecord(adapter.getName(),receiver.getName(),null);
							String type = errorStorage.getType();
							slotmap.put(type+"/"+slotId,sir);
						}
					}
					ITransactionalStorage messageLog=receiver.getMessageLog();
					if (messageLog!=null) {
						String slotId=messageLog.getSlotId();
						if (StringUtils.isNotEmpty(slotId)) {
							SlotIdRecord sir=new SlotIdRecord(adapter.getName(),receiver.getName(),null);
							String type = messageLog.getType();
							slotmap.put(type+"/"+slotId,sir);
						}
					}
				}
				PipeLine pipeline=adapter.getPipeLine();
				if (pipeline!=null) {
					for (int i=0; i<pipeline.getPipeLineSize(); i++) {
						IPipe pipe=pipeline.getPipe(i);
						if (pipe instanceof MessageSendingPipe) {
							MessageSendingPipe msp=(MessageSendingPipe)pipe;
							ITransactionalStorage messageLog = msp.getMessageLog();
							if (messageLog!=null) {
								String slotId=messageLog.getSlotId();
								if (StringUtils.isNotEmpty(slotId)) {
									SlotIdRecord sir=new SlotIdRecord(adapter.getName(),null,msp.getName());
									String type = messageLog.getType();
									slotmap.put(type+"/"+slotId,sir);
									slotmap.put(slotId,sir);
								}
							}
						}
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

			String formQuery=AppConstants.getInstance().getProperty(SHOWIBISSTOREQUERYKEY);

			DirectQuerySender qs;
			String result = "<none/>";

			try {
				qs = new DirectQuerySender() {
					protected String getResult(ResultSet resultset) throws JdbcException, SQLException, IOException {
						return getResult(resultset,null,null);
					}
					protected String getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar) throws JdbcException, SQLException, IOException {
						XmlBuilder result = new XmlBuilder("result");
						String previousType=null;
						XmlBuilder typeXml=null;
						String previousSlot=null;
						XmlBuilder slotXml=null;
						int typeslotcount=0;
						int typedatecount=0;
						int typemsgcount=0;
						int slotdatecount=0;
						int slotmsgcount=0;
						while (resultset.next()) {
							String type = resultset.getString("type");
							String slotid = resultset.getString("slotid");
							String date =  resultset.getString("msgdate");
							int count =    resultset.getInt("msgcount");
							
							if (type==null) {
								type="";
							}
							if (slotid==null) {
								slotid="";
							}
						
							if (!type.equals(previousType)) {
								if (typeXml!=null) {
									typeXml.addAttribute("slotcount",typeslotcount);
									typeXml.addAttribute("datecount",typedatecount);
									typeXml.addAttribute("msgcount",typemsgcount);
									typeslotcount=0;
									typedatecount=0;
									typemsgcount=0;
									previousSlot=null;
								}
								typeXml=new XmlBuilder("type");
								typeXml.addAttribute("id",type);
								if (type.equalsIgnoreCase("E")) {
									typeXml.addAttribute("name","errorlog");
								} else {
									typeXml.addAttribute("name","messagelog");
								}
								result.addSubElement(typeXml);
								previousType=type;
							}
							if (!slotid.equals(previousSlot)) {
								if (slotXml!=null) {
									slotXml.addAttribute("datecount",slotdatecount);
									slotXml.addAttribute("msgcount",slotmsgcount);
									slotdatecount=0;
									slotmsgcount=0;
								}
								slotXml=new XmlBuilder("slot");
								slotXml.addAttribute("id",slotid);
								if (StringUtils.isNotEmpty(slotid)) {
									SlotIdRecord sir=(SlotIdRecord)slotmap.get(type+"/"+slotid);
									if (sir!=null) {
										slotXml.addAttribute("adapter",sir.adapterName);
										if (StringUtils.isNotEmpty(sir.receiverName) ) {
											slotXml.addAttribute("receiver",sir.receiverName);
										}
										if (StringUtils.isNotEmpty(sir.pipeName) ) {
											slotXml.addAttribute("pipe",sir.pipeName);
										}
									}
								}
								typeXml.addSubElement(slotXml);
								previousSlot=slotid;
								typeslotcount++;
							}
							typemsgcount+=count;
							typedatecount++;
							slotmsgcount+=count;
							slotdatecount++;
							
							XmlBuilder dateXml=new XmlBuilder("date");
							dateXml.addAttribute("id",date);
							dateXml.addAttribute("count",count);
							slotXml.addSubElement(dateXml);
						}
						if (typeXml!=null) {
							typeXml.addAttribute("slotcount",typeslotcount);
							typeXml.addAttribute("datecount",typedatecount);
							typeXml.addAttribute("msgcount",typemsgcount);
						}
						if (slotXml!=null) {
							slotXml.addAttribute("datecount",slotdatecount);
							slotXml.addAttribute("msgcount",slotmsgcount);
							slotdatecount=0;
							slotmsgcount=0;
						}
						return result.toXML();
					}
				};
				try {
					qs.setName("QuerySender");
					qs.setJmsRealm(jmsRealm);
					qs.setQueryType("select");
					qs.setBlobSmartGet(true);
					qs.configure();
					qs.open();
					result = qs.sendMessage("dummy", formQuery);
				} catch (Throwable t) {
					error("error occured on executing jdbc query",t);
				} finally {
					qs.close();
				}
			} catch (Exception e) {
				error("error occured on creating or closing connection",e);
			}
			if (log.isDebugEnabled()) log.debug("result ["+result+"]");
			request.setAttribute("result", result);

		}


		if (!errors.isEmpty()) {
			saveErrors(request, errors);
			return (mapping.findForward("success"));
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
