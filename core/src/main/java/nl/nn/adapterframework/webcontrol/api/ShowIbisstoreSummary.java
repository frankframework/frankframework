/*
Copyright 2016-2017, 2019, 2020 WeAreFrank!

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
				qs.setAvoidLocking(true);
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

		String resultObject = "{ \"result\":"+result+"}";
		
		return Response.status(Response.Status.CREATED).entity(resultObject).build();
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
		JsonObjectBuilder typeBuilder=null;
		JsonArrayBuilder slotsBuilder=null;
		String previousSlot=null;
		JsonObjectBuilder slotBuilder=null;
		JsonArrayBuilder datesBuilder=null;
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
				if (typeBuilder!=null) {
					slotBuilder.add("datecount",slotdatecount);
					slotBuilder.add("msgcount",slotmsgcount);
					slotBuilder.add("dates", datesBuilder.build());
					slotsBuilder.add(slotBuilder.build());
					slotdatecount=0;
					slotmsgcount=0;
					typeBuilder.add("slotcount",typeslotcount);
					typeBuilder.add("datecount",typedatecount);
					typeBuilder.add("msgcount",typemsgcount);
					typeBuilder.add("slots", slotsBuilder.build());
					types.add(typeBuilder.build());
					typeslotcount=0;
					typedatecount=0;
					typemsgcount=0;
					previousSlot=null;
					slotBuilder=null;
				}
				typeBuilder = Json.createObjectBuilder();
				typeBuilder.add("type", type);
				if (type.equalsIgnoreCase("E")) {
					typeBuilder.add("name","errorlog");
				} else {
					typeBuilder.add("name","messagelog");
				}
				slotsBuilder = Json.createArrayBuilder();
				previousType=type;
			}
			if (!slotid.equals(previousSlot)) {
				if (slotBuilder!=null) {
					slotBuilder.add("datecount",slotdatecount);
					slotBuilder.add("msgcount",slotmsgcount);
					slotBuilder.add("dates", datesBuilder.build());
					slotsBuilder.add(slotBuilder.build());
					slotdatecount=0;
					slotmsgcount=0;
				} 
				slotBuilder=Json.createObjectBuilder();
				datesBuilder = Json.createArrayBuilder();
				slotBuilder.add("id",slotid);
				if (StringUtils.isNotEmpty(slotid)) {
					SlotIdRecord sir=(SlotIdRecord)slotmap.get(type+"/"+slotid);
					if (sir!=null) {
						slotBuilder.add("adapter",sir.adapterName);
						if (StringUtils.isNotEmpty(sir.receiverName) ) {
							slotBuilder.add("receiver",sir.receiverName);
						}
						if (StringUtils.isNotEmpty(sir.pipeName) ) {
							slotBuilder.add("pipe",sir.pipeName);
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
			
			datesBuilder.add(Json.createObjectBuilder().add("id",date).add("count",count).build());
		}

		if (slotBuilder!=null) {
			slotBuilder.add("datecount",slotdatecount);
			slotBuilder.add("msgcount",slotmsgcount);
			slotBuilder.add("dates", datesBuilder.build());
			slotsBuilder.add(slotBuilder.build());
		}
		
		if (typeBuilder!=null) {
			typeBuilder.add("slotcount",typeslotcount);
			typeBuilder.add("datecount",typedatecount);
			typeBuilder.add("msgcount",typemsgcount);
			typeBuilder.add("slots", slotsBuilder.build());
			types.add(typeBuilder.build());
		}
		
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
