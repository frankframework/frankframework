/*
Copyright 2016 Integration Partners B.V.

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.ITransactionalStorage;
import nl.nn.adapterframework.core.PipeLine;
import nl.nn.adapterframework.jdbc.DirectQuerySender;
import nl.nn.adapterframework.jdbc.JdbcException;
import nl.nn.adapterframework.pipes.MessageSendingPipe;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;

/**
* Executes a query.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowIbisstoreSummary extends Base {
	@Context ServletConfig servletConfig;

	public static final String SHOWIBISSTOREQUERYKEY="ibisstore.summary.query";

	@POST
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/jdbc/summary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException {
		initBase(servletConfig);

		Response.ResponseBuilder response = Response.noContent(); //PUT defaults to no content

		String query = AppConstants.getInstance().getProperty(SHOWIBISSTOREQUERYKEY);
		String realm = null;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("realm")) {
				realm = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("query")) {
				query = entry.getValue().toString();
			}
		}

		if(realm == null)
			return response.status(Response.Status.BAD_REQUEST).build();

		String result = "";
		try {
			IbisstoreSummaryQuerySender qs;
			qs = (IbisstoreSummaryQuerySender) ibisManager.getIbisContext().createBeanAutowireByName(IbisstoreSummaryQuerySender.class);
			qs.setSlotmap(getSlotmap());
			try {
				qs.setName("QuerySender");
				qs.setJmsRealm(realm);
				qs.setQueryType("select");
				qs.setBlobSmartGet(true);
				qs.configure(true);
				qs.open();
				result = qs.sendMessage("dummy", query);
			} catch (Throwable t) {
				return buildErrorResponse("An error occured on executing jdbc query: "+t.toString());
			} finally {
				qs.close();
			}
		} catch (Exception e) {
			return buildErrorResponse("An error occured on creating or closing the connection: "+e.toString());
		}

		List<Map<String, String>> resultMap = null;
		if(XmlUtils.isWellFormed(result)) {
			resultMap = Xml2Map(result);
		}
		if(resultMap == null)
			return buildErrorResponse("Invalid query result.");

		Map<String, Object> resultObject = new HashMap<String, Object>();
		resultObject.put("query", query);
		resultObject.put("result", resultMap);

		return Response.status(Response.Status.CREATED).entity(resultObject).build();
	}

	private Map<String, SlotIdRecord> getSlotmap() {
		Map<String, SlotIdRecord> slotmap = new HashMap<String, SlotIdRecord>();

		for(IAdapter iAdapter : ibisManager.getRegisteredAdapters()) {
			Adapter adapter = (Adapter)iAdapter;
			for(Iterator<?> receiverIt=adapter.getReceiverIterator(); receiverIt.hasNext();) {
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
		return slotmap;
	}
}

class IbisstoreSummaryQuerySender extends DirectQuerySender {
	private Map<String, SlotIdRecord> slotmap = new HashMap<String, SlotIdRecord>();
	
	public void setSlotmap(Map<String, SlotIdRecord> slotmap) {
		this.slotmap = slotmap;
	}

	@Override
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
}

class SlotIdRecord {
	public String adapterName;
	public String receiverName;
	public String pipeName;
	
	SlotIdRecord(String adapterName, String receiverName, String pipeName) {
		super();
		this.adapterName=adapterName;
		this.receiverName=receiverName;
		this.pipeName=pipeName;
	}
}
