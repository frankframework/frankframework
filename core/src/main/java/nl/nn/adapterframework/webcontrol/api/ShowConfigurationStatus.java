/*
   Copyright 2016-2022 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
import nl.nn.adapterframework.receivers.Receiver;

/**
 * Get adapter information from either all or a specified adapter
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowConfigurationStatus extends Base {

	private Adapter getAdapter(String adapterName) {
		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		return adapter;
	}

	private String getConfigurationNameByAdapter(String adapterName) {
		Adapter adapter = getIbisManager().getRegisteredAdapter(adapterName);

		if(adapter == null){
			throw new ApiException("Adapter not found!");
		}

		return adapter.getConfiguration().getName();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapters(@QueryParam("expanded") String expanded, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.GET);
		builder.addHeader("showPendingMsgCount", showPendingMsgCount);
		builder.addHeader("expanded", expanded);
		return callSyncGateway(builder, true);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterOld(@PathParam("name") String name, @QueryParam("configuration") String configuration, @QueryParam("expanded") String expanded, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) {
		if(StringUtils.isNotEmpty(configuration)) {
			return getAdapterNew(configuration, name, expanded, showPendingMsgCount);
		}
		throw new ApiException("either provide the configuration as query param or use endpoint /configurations/<config>/adapters/"+name, Status.MOVED_PERMANENTLY);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAdapterNew(@PathParam("configuration") String configuration, @PathParam("name") String name, @QueryParam("expanded") String expanded, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.FIND);
		builder.addHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, name);

		builder.addHeader("showPendingMsgCount", showPendingMsgCount);
		builder.addHeader("expanded", expanded);
		return callSyncGateway(builder, true);
	}

	@GET
	@PermitAll
	@Path("/adapters/{name}/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getIbisHealth(@PathParam("name") String name) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.HEALTH);
		builder.addHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfigurationNameByAdapter(name));
		builder.addHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, name);

		return callSyncGateway(builder);
	}

	@SuppressWarnings("unchecked")
	@PUT //Normally you don't use the PUT method on a collection...
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateAdapters(Map<String, Object> json) {

		IbisAction action = null;
		ArrayList<String> adapters = new ArrayList<>();

		String value = getValue(json, "action");
		if(StringUtils.isNotEmpty(value)) {
			if(value.equals("stop")) { action = IbisAction.STOPADAPTER; }
			if(value.equals("start")) { action = IbisAction.STARTADAPTER; }
		}
		if(action == null) {
			throw new ApiException("no or unknown action provided", Response.Status.BAD_REQUEST);
		}

		Object adapterList = json.get("adapters");
		if(adapterList != null) {
			try {
				adapters.addAll((ArrayList<String>) adapterList);
			} catch(Exception e) {
				throw new ApiException(e);
			}
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
		builder.addHeader("action", action.name());
		if(adapters.isEmpty()) {
			builder.addHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, "*ALL*");
			builder.addHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, "*ALL*");
			callAsyncGateway(builder);
		} else {
			for (Iterator<String> iterator = adapters.iterator(); iterator.hasNext();) {
				String adapterName = iterator.next();
				builder.addHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfigurationNameByAdapter(adapterName));
				builder.addHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, adapterName);
				callAsyncGateway(builder);
			}
		}

		return Response.status(Response.Status.ACCEPTED).build(); //PUT defaults to no content
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateAdapter(@PathParam("adapterName") String adapterName, Map<String, Object> json) {

		getAdapter(adapterName); //Check if the adapter exists!

		Object value = json.get("action");
		if(value instanceof String) {
			IbisAction action = null;
			if(value.equals("stop")) { action = IbisAction.STOPADAPTER; }
			if(value.equals("start")) { action = IbisAction.STARTADAPTER; }
			if(action == null) {
				throw new ApiException("no or unknown action provided", Response.Status.BAD_REQUEST);
			}

			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
			builder.addHeader("action", action.name());
			builder.addHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfigurationNameByAdapter(adapterName));
			builder.addHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, adapterName);
			callAsyncGateway(builder);
			return Response.status(Response.Status.ACCEPTED).entity("{\"status\":\"ok\"}").build();
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapterName}/receivers/{receiverName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateReceiver(@PathParam("adapterName") String adapterName, @PathParam("receiverName") String receiverName, Map<String, Object> json) {

		Adapter adapter = getAdapter(adapterName);
		Receiver<?> receiver = adapter.getReceiverByName(receiverName);
		if(receiver == null) {
			throw new ApiException("Receiver ["+receiverName+"] not found!");
		}

		Object value = json.get("action");
		if(value instanceof String) {
			IbisAction action = null;
			if(value.equals("stop")) { action = IbisAction.STOPRECEIVER; }
			else if(value.equals("start")) { action = IbisAction.STARTRECEIVER; }
			else if(value.equals("incthread")) { action = IbisAction.INCTHREADS; }
			else if(value.equals("decthread")) { action = IbisAction.DECTHREADS; }
			if(action == null) {
				throw new ApiException("no or unknown action provided", Response.Status.BAD_REQUEST);
			}

			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
			builder.addHeader("action", action.name());
			builder.addHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfigurationNameByAdapter(adapterName));
			builder.addHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, adapterName);
			builder.addHeader("receiver", receiverName);
			callAsyncGateway(builder);
			return Response.status(Response.Status.ACCEPTED).entity("{\"status\":\"ok\"}").build();
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{name}/flow")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public Response getAdapterFlow(@PathParam("name") String adapterName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FLOW);
		builder.addHeader(FrankApiBase.HEADER_CONFIGURATION_NAME_KEY, getConfigurationNameByAdapter(adapterName));
		builder.addHeader(FrankApiBase.HEADER_ADAPTER_NAME_KEY, adapterName);
		return callSyncGateway(builder);
	}
}
