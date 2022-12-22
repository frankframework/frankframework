/*
   Copyright 2016-2020 WeAreFrank!

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
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;

/**
 * Retrieves the Scheduler metadata and the jobgroups with there jobs from the Scheduler.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowScheduler extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedules() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.GET));
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/jobs/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedule(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.FIND);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateScheduler(Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.MANAGE);
		builder.addHeader("operation", getValue(json, "action"));
		return callSyncGateway(builder);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/jobs/{jobName}")
	@Relation("schedules")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response trigger(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.MANAGE);
		builder.addHeader("operation", getValue(json, "action"));
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}


	// Database jobs

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createSchedule(MultipartBody input) {
		String jobGroupName = resolveStringFromMap(input, "group");
		return createSchedule(jobGroupName, input);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/jobs/{jobName}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	public Response updateSchedule(@PathParam("groupName") String groupName, @PathParam("jobName") String jobName, MultipartBody input) {
		return createSchedule(groupName, jobName, input, true);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/jobs")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createScheduleInJobGroup(@PathParam("groupName") String groupName, MultipartBody input) {
		return createSchedule(groupName, input);
	}

	private Response createSchedule(String groupName, MultipartBody input) {
		String jobName = resolveStringFromMap(input, "name");
		return createSchedule(groupName, jobName, input, false);
	}

	private Response createSchedule(String groupName, String jobName, MultipartBody inputDataMap, boolean overwrite) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.UPLOAD);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);

		builder.addHeader("cron", resolveTypeFromMap(inputDataMap, "cron", String.class, ""));
		builder.addHeader("interval", resolveTypeFromMap(inputDataMap, "interval", Integer.class, -1));

		builder.addHeader("adapter", resolveStringFromMap(inputDataMap, "adapter"));
		builder.addHeader("receiver", resolveTypeFromMap(inputDataMap, "receiver", String.class, ""));
		builder.addHeader("configuration", resolveTypeFromMap(inputDataMap, HEADER_CONFIGURATION_NAME_KEY, String.class, ""));
		builder.addHeader("listener", resolveTypeFromMap(inputDataMap, "listener", String.class, ""));

		builder.addHeader("persistent", resolveTypeFromMap(inputDataMap, "persistent", boolean.class, false));
		builder.addHeader("locker", resolveTypeFromMap(inputDataMap, "locker", boolean.class, false));
		builder.addHeader("lockkey", resolveTypeFromMap(inputDataMap, "lockkey", String.class, "lock4["+jobName+"]"));

		builder.addHeader("message", resolveStringFromMap(inputDataMap, "message"));

		builder.addHeader("description", resolveStringFromMap(inputDataMap, "description"));
		if(overwrite) {
			builder.addHeader("overwrite", overwrite);
		}

		return callSyncGateway(builder);
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/jobs/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response deleteSchedules(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.DELETE);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}
}
