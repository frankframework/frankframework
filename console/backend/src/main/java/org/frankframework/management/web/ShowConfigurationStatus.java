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
package org.frankframework.management.web;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.management.IbisAction;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

import org.frankframework.util.RequestUtils;

/**
 * Get adapter information from either all or a specified adapter
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowConfigurationStatus extends FrankApiBase {
	private static final String REDIRECT_MESSAGE_PREFIX = "either provide the configuration as query param or use endpoint /configurations/<config>";

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("adapter")
	@Description("view a list of all adapters")
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
	@Deprecated
	public Response getAdapterOld(@PathParam("name") String name, @QueryParam("configuration") String configuration, @QueryParam("expanded") String expanded, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) {
		if(StringUtils.isNotEmpty(configuration)) {
			return getAdapter(configuration, name, expanded, showPendingMsgCount);
		}
		throw new ApiException(REDIRECT_MESSAGE_PREFIX+"/adapters/"+name, Status.BAD_REQUEST);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{name}")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("adapter")
	@Description("view an adapter receivers/pipes/messages")
	public Response getAdapter(@PathParam("configuration") String configuration, @PathParam("name") String name, @QueryParam("expanded") String expanded, @QueryParam("showPendingMsgCount") boolean showPendingMsgCount) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.FIND);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, name);

		builder.addHeader("showPendingMsgCount", showPendingMsgCount);
		builder.addHeader("expanded", expanded);
		return callSyncGateway(builder, true);
	}

	@GET
	@PermitAll
	@Path("/adapters/{name}/health")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response getIbisHealthOld(@PathParam("name") String name, @QueryParam("configuration") String configuration) {
		if(StringUtils.isNotEmpty(configuration)) {
			return getAdapterHealth(configuration, name);
		}
		throw new ApiException(REDIRECT_MESSAGE_PREFIX+"/adapters/"+name+"/health", Status.BAD_REQUEST);
	}

	@GET
	@PermitAll
	@Path("/configurations/{configuration}/adapters/{name}/health")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("adapter")
	@Description("view an adapter health")
	public Response getAdapterHealth(@PathParam("configuration") String configuration, @PathParam("name") String name) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.HEALTH);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, name);

		return callSyncGateway(builder);
	}

	@SuppressWarnings("unchecked")
	@PUT //Normally you don't use the PUT method on a collection...
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("adapter")
	@Description("start/stop multiple adapters")
	public Response updateAdapters(Map<String, Object> json) {

		IbisAction action = null;
		ArrayList<String> adapters = new ArrayList<>();

		String value = RequestUtils.getValue(json, "action");
		if(StringUtils.isNotEmpty(value)) {
			if("stop".equals(value)) { action = IbisAction.STOPADAPTER; }
			if("start".equals(value)) { action = IbisAction.STARTADAPTER; }
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
			builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, "*ALL*");
			builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, "*ALL*");
			callAsyncGateway(builder);
		} else {
			for (Iterator<String> iterator = adapters.iterator(); iterator.hasNext();) {
				String adapterNameWithPossibleConfigurationName = iterator.next();
				int slash = adapterNameWithPossibleConfigurationName.indexOf("/");
				String adapterName;
				if(slash > -1) {
					adapterName = adapterNameWithPossibleConfigurationName.substring(slash+1);
					String configurationName = adapterNameWithPossibleConfigurationName.substring(0, slash);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
				} else {
					adapterName = adapterNameWithPossibleConfigurationName;
				}
				builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapterName);
				callAsyncGateway(builder);
			}
		}

		return Response.status(Response.Status.ACCEPTED).build(); //PUT defaults to no content
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapter}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response updateAdapterOld(@PathParam("adapter") String adapter, @QueryParam("configuration") String configuration, Map<String, Object> json) {
		if(StringUtils.isNotEmpty(configuration)) {
			return updateAdapter(configuration, adapter, json);
		}
		throw new ApiException(REDIRECT_MESSAGE_PREFIX+"/adapters/"+adapter, Status.BAD_REQUEST);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapter}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("adapter")
	@Description("start/stop an adapter")
	public Response updateAdapter(@PathParam("configuration") String configuration, @PathParam("adapter") String adapter, Map<String, Object> json) {
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
			builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
			builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
			callAsyncGateway(builder);
			return Response.status(Response.Status.ACCEPTED).entity("{\"status\":\"ok\"}").build();
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapter}/receivers/{receiver}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response updateReceiverOld(@QueryParam("configuration") String configuration, @PathParam("adapter") String adapter, @PathParam("receiver") String receiver, Map<String, Object> json) {
		if(StringUtils.isNotEmpty(configuration)) {
			return updateReceiverOld(configuration, adapter, receiver, json);
		}
		throw new ApiException(REDIRECT_MESSAGE_PREFIX+"/adapters/"+adapter+"/receivers/"+receiver, Status.BAD_REQUEST);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapter}/receivers/{receiver}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("adapter")
	@Description("start/stop an adapter receivers")
	public Response updateReceiver(@PathParam("configuration") String configuration, @PathParam("adapter") String adapter, @PathParam("receiver") String receiver, Map<String, Object> json) {
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
			builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
			builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
			builder.addHeader("receiver", receiver);
			callAsyncGateway(builder);
			return Response.status(Response.Status.ACCEPTED).entity("{\"status\":\"ok\"}").build();
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapter}/flow")
	@Produces(MediaType.TEXT_PLAIN)
	@Deprecated
	public Response getAdapterFlowOld(@PathParam("adapter") String adapter, @QueryParam("configuration") String configuration) {
		if(StringUtils.isNotEmpty(configuration)) {
			return getAdapterFlow(configuration, adapter);
		}
		throw new ApiException(REDIRECT_MESSAGE_PREFIX+"/adapters/"+adapter+"/flow", Status.BAD_REQUEST);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapter}/flow")
	@Relation("adapter")
	@Description("view an adapter flow")
	public Response getAdapterFlow(@PathParam("configuration") String configuration, @PathParam("adapter") String adapter) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FLOW);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		return callSyncGateway(builder);
	}
}
