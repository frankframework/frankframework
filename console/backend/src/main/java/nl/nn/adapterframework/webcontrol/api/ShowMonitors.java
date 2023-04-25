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
package nl.nn.adapterframework.webcontrol.api;

import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.web.FrankApiBase;
import nl.nn.adapterframework.management.web.RequestMessageBuilder;

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
	public Response getMonitors(@PathParam("configuration") String configurationName, @DefaultValue("false") @QueryParam("xml") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("xml", showConfigXml);
		return callSyncGateway(builder);
	}

	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces()
	public Response getMonitor(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @DefaultValue("false") @QueryParam("xml") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader("xml", showConfigXml);
		return callSyncGateway(builder, true);
	}

	@PUT
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateMonitor(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.MANAGE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader(MONITOR_HEADER, monitorName);

		String state = String.valueOf(json.remove("state"));
		if(state != null) {
			builder.addHeader("state", state);
		}
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}

	@DELETE
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteMonitor(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.DELETE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response createTrigger(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}

	@GET
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{triggerId}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getTriggers(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @PathParam("triggerId") Integer id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader(TRIGGER_HEADER, id);
		return callSyncGateway(builder, true);
	}

	@PUT
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateTrigger(@PathParam("configuration") String configName, @PathParam("monitorName") String monitorName, @PathParam("trigger") int index, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.MANAGE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader(TRIGGER_HEADER, index);
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}

	@DELETE
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/{monitorName}/triggers/{trigger}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteTrigger(@PathParam("configuration") String configurationName, @PathParam("monitorName") String monitorName, @PathParam("trigger") int id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.DELETE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader(MONITOR_HEADER, monitorName);
		builder.addHeader(TRIGGER_HEADER, id);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({ "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	@Path("/")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response addMonitor(@PathParam("configuration") String configurationName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.MONITORING, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);

		// Map 'monitor' to 'name', so it matches the DTO.
		String monitor = String.valueOf(json.remove("monitor"));
		json.put("name", monitor);
		builder.setJsonPayload(json);

		return callSyncGateway(builder);
	}
}
