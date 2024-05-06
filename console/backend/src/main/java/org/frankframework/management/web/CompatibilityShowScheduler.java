/*
   Copyright 2023 WeAreFrank!

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
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

import org.frankframework.util.RequestUtils;

/**
 * This class exists to provide backwards compatibility for endpoints from before #4069.
 *
 * @since	7.8.1
 * @author	Niels Meijer
 */
@Path("/")
public final class CompatibilityShowScheduler extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response getScheduleOld(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.FIND);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Relation("schedules")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response getTriggerOld(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.MANAGE);
		builder.addHeader("operation", RequestUtils.getValue(json, "action"));
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}


	// Database jobs

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response updateScheduleOld(@PathParam("groupName") String groupName, @PathParam("jobName") String jobName, MultipartBody input) {
		return ShowScheduler.createSchedule(this, groupName, jobName, input, true);
	}

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response createScheduleInJobGroupOld(@PathParam("groupName") String groupName, MultipartBody input) {
		String jobName = RequestUtils.resolveStringFromMap(input, "name");
		return ShowScheduler.createSchedule(this, groupName, jobName, input, false);
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/schedules/{groupName}/job/{jobName}")
	@Relation("schedules")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response deleteSchedulesOld(@PathParam("jobName") String jobName, @PathParam("groupName") String groupName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.DELETE);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}
}
