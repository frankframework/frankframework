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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IForwardTarget;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CookieUtil;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.webcontrol.IniDynaActionForm;

/**
 * Show counts per day of each slot in the ibisstore.
 * 
 * @author  Gerrit van Brakel 
 * @since	4.9.7
 */

public class ShowIbisstoreSummary extends ActionBase {

	public static final String SHOWIBISSTORECOOKIE="ShowIbisstoreSummaryCookieName";

	private Map slotmap = new HashMap();

	public ActionForward executeSub(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		IniDynaActionForm showIbisstoreSummaryForm = (IniDynaActionForm) form;
		// Initialize action
		initAction(request);
		if(ibisManager==null)return (mapping.findForward("noIbisContext"));

		String jmsRealm = (String) showIbisstoreSummaryForm.get("jmsRealm");
		String cookieName=AppConstants.getInstance().getString(SHOWIBISSTORECOOKIE,SHOWIBISSTORECOOKIE);

		if (StringUtils.isEmpty(jmsRealm)) {
			// get jmsRealm value from cookie
			Cookie cookie = CookieUtil.getCookie(request, cookieName);
			if (null != cookie) {
				jmsRealm = cookie.getValue();
				log.debug("jmsRealm from cookie [" + jmsRealm +"]");
			}
		}

		for(Adapter adapter: ibisManager.getRegisteredAdapters()) {
			for (Receiver receiver: adapter.getReceivers()) {
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
			String result = "<none/>";

			try {
				IbisstoreSummaryQuerySender qs;
				qs = (IbisstoreSummaryQuerySender)ibisManager.getIbisContext().createBeanAutowireByName(IbisstoreSummaryQuerySender.class);
				qs.setSlotmap(slotmap);
				try {
					qs.setName("QuerySender");
					qs.setJmsRealm(jmsRealm);
					qs.setQueryType("select");
					qs.setBlobSmartGet(true);
					qs.configure(true);
					qs.open();
					result = qs.sendMessage(new Message(qs.getDbmsSupport().getIbisStoreSummaryQuery()), null).asString();
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
		cookie.setHttpOnly(true);
		cookie.setSecure(true);
		log.debug("Store cookie for " + request.getServletPath() + " cookieName[" + cookieName + "] " + " cookieValue[" + cookieValue + "]");
		try {
			response.addCookie(cookie);
		} catch (Throwable t) {
			log.warn("unable to add cookie to request. cookie value [" + cookie.getValue() + "]",t);
		}

		log.debug("forward to success");
		return (mapping.findForward("success"));

	}
}

class IbisstoreSummaryQuerySender extends DirectQuerySender {
	private Map slotmap = new HashMap();
	
	public void setSlotmap(Map slotmap) {
		this.slotmap = slotmap;
	}

	@Override
	protected PipeRunResult getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition, IPipeLineSession session, IForwardTarget next) throws JdbcException, SQLException, IOException {
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
		return new PipeRunResult(null, new Message(result.toXML()));
	}
}

class SlotIdRecord {
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
