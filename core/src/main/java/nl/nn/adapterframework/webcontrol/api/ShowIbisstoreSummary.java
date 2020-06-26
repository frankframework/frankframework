/*
Copyright 2016-2017, 2019 Integration Partners B.V.

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
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.json.JsonStructure;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
import nl.nn.adapterframework.stream.Message;

@Path("/")
public final class ShowIbisstoreSummary extends Base {

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/summary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException {

		Response.ResponseBuilder response = Response.noContent(); //PUT defaults to no content

		String query = null;
		String datasource = null;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("datasource")) {
				datasource = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("query")) {
				query = entry.getValue().toString();
			}
		}

		if(datasource == null)
			return response.status(Response.Status.BAD_REQUEST).build();

		String result = "";
		try {
			IbisstoreSummaryQuerySender qs;
			qs = (IbisstoreSummaryQuerySender) getIbisContext().createBeanAutowireByName(IbisstoreSummaryQuerySender.class);
			qs.setSlotmap(getSlotmap());
			try {
				qs.setName("QuerySender");
				qs.setDatasourceName(datasource);
				qs.setQueryType("select");
				qs.setBlobSmartGet(true);
				qs.configure(true);
				qs.open();
				result = qs.sendMessage(new Message(query!=null?query:qs.getDbmsSupport().getIbisStoreSummaryQuery()), null).asString();
			} catch (Throwable t) {
				throw new ApiException("An error occured on executing jdbc query", t);
			} finally {
				qs.close();
			}
		} catch (Exception e) {
			throw new ApiException("An error occured on creating or closing the connection", e);
		}

		System.out.println("--->"+result);

//		Map<String, Object> resultObject = new HashMap<String, Object>();
//		resultObject.put("query", query);
//		resultObject.put("result", result);

		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	private Map<String, SlotIdRecord> getSlotmap() {
		Map<String, SlotIdRecord> slotmap = new HashMap<String, SlotIdRecord>();

		for(IAdapter iAdapter : getIbisManager().getRegisteredAdapters()) {
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
	protected Message getResult(ResultSet resultset, Object blobSessionVar, Object clobSessionVar, HttpServletResponse response, String contentType, String contentDisposition) throws JdbcException, SQLException, IOException {
		JsonArrayBuilder types = Json.createArrayBuilder();
		String previousType=null;
		JsonObjectBuilder typeXml=null;
		JsonArrayBuilder slots=null;
		String previousSlot=null;
		JsonObjectBuilder slotXml=null;
		JsonArrayBuilder dates=null;
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
					slotXml.add("datecount",slotdatecount);
					slotXml.add("msgcount",slotmsgcount);
					slotXml.add("dates", dates.build());
					slotdatecount=0;
					slotmsgcount=0;
					slots.add(slotXml.build());
					typeXml.add("slotcount",typeslotcount);
					typeXml.add("datecount",typedatecount);
					typeXml.add("msgcount",typemsgcount);
					typeXml.add("slots", slots.build());
					types.add(typeXml.build());
					typeslotcount=0;
					typedatecount=0;
					typemsgcount=0;
					previousSlot=null;
					slotXml=null;
				}
				typeXml = Json.createObjectBuilder();
				typeXml.add("type", type);
				if (type.equalsIgnoreCase("E")) {
					typeXml.add("name","errorlog");
				} else {
					typeXml.add("name","messagelog");
				}
				slots = Json.createArrayBuilder();
				previousType=type;
			}
			if (!slotid.equals(previousSlot)) {
				if (slotXml!=null) {
					slotXml.add("datecount",slotdatecount);
					slotXml.add("msgcount",slotmsgcount);
					slotXml.add("dates", dates.build());
					slotdatecount=0;
					slotmsgcount=0;
					slots.add(slotXml.build());
				} 
				slotXml=Json.createObjectBuilder();
				dates = Json.createArrayBuilder();
				slotXml.add("id",slotid);
				if (StringUtils.isNotEmpty(slotid)) {
					SlotIdRecord sir=(SlotIdRecord)slotmap.get(type+"/"+slotid);
					if (sir!=null) {
						slotXml.add("adapter",sir.adapterName);
						if (StringUtils.isNotEmpty(sir.receiverName) ) {
							slotXml.add("receiver",sir.receiverName);
						}
						if (StringUtils.isNotEmpty(sir.pipeName) ) {
							slotXml.add("pipe",sir.pipeName);
						}
					}
				}
				previousSlot=slotid;
				typeslotcount++;
			}
			typemsgcount+=count;
			typedatecount++;
			slotmsgcount+=count;
			slotdatecount++;
			
			dates.add(Json.createObjectBuilder().add("id",date).add("count",count).build());
		}

		slotXml.add("datecount",slotdatecount);
		slotXml.add("msgcount",slotmsgcount);
		slotXml.add("dates", dates.build());
		slots.add(slotXml.build());
		
		typeXml.add("slotcount",typeslotcount);
		typeXml.add("datecount",typedatecount);
		typeXml.add("msgcount",typemsgcount);
		typeXml.add("slots", slots.build());
		types.add(typeXml.build());

		JsonStructure result = types.build();
		return new Message(result.toString());
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
