/*
   Copyright 2016-2023 WeAreFrank!

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

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

/**
 * Shows all monitors.
 *
 * @since 7.0-B1
 * @author Niels Meijer
 */

@Path("/configurations/{configuration}/monitors")
public class ShowMonitors extends FrankApiBase {
	private static final String MONITOR_HEADER = "monitor";
	private static final String TRIGGER_HEADER = "trigger";

	@GET
	@Path("/")
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Relation("monitoring")
	@Description("view all available monitors")
	public Response getMonitors(@PathParam("configuration") String configurationName, @DefaultValue("false") @QueryParam("xml") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("xml", showConfigXml);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("add a new monitor")
	public Response addMonitor(@PathParam("configuration") String configurationName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);

		// Map 'monitor' to 'name', so it matches the DTO.
		String monitor = String.valueOf(json.remove("monitor"));
		json.put("name", monitor);
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}


	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Relation("monitoring")
	@Description("get a specific monitor")
	public Response getMonitor(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @DefaultValue("false") @QueryParam("xml") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader("xml", showConfigXml);
		return callSyncGateway(builder, true);
	}

	@PUT
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("update a specific monitor")
	public Response updateMonitor(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.MANAGE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader(MONITOR_HEADER, monitorName);

		Object state = json.remove("action");
		if(state != null) {
			builder.addHeader("state", String.valueOf(state));
		}
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}

	@DELETE
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("delete a specific monitor")
	public Response deleteMonitor(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.DELETE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		return callSyncGateway(builder);
	}


	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("view specific monitor")
	public Response getTriggers(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		return callSyncGateway(builder, true);
	}

	@POST
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("update a specific monitors triggers")
	public Response addTrigger(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}


	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("view all triggers for a specific monitor")
	public Response getTrigger(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @PathParam("trigger") Integer id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader(TRIGGER_HEADER, id);
		return callSyncGateway(builder, true);
	}

	@PUT
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("update a specific monitor triggers")
	public Response updateTrigger(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, @PathParam("trigger") int index, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.MANAGE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader(TRIGGER_HEADER, index);
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}

	@DELETE
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("monitoring")
	@Description("delete a specific monitor trigger")
	public Response deleteTrigger(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @PathParam("trigger") int id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.DELETE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader(TRIGGER_HEADER, id);
		return callSyncGateway(builder);
	}
}
