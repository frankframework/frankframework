/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.console.controllers;


import jakarta.annotation.security.RolesAllowed;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@RestController
public class Scheduler {

	private final FrankApiService frankApiService;

	public Scheduler(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@Relation("schedules")
	@GetMapping(value = "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getSchedules() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.SCHEDULER, BusAction.GET));
	}

	@AllowAllIbisUserRoles
	@Relation("schedules")
	@GetMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getSchedule(SchedulerPathVariables path) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SCHEDULER, BusAction.FIND);
		builder.addHeader("job", path.jobName);
		builder.addHeader("group", path.groupName);
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PutMapping(value = "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateScheduler(@RequestBody SchedulerModel json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SCHEDULER, BusAction.MANAGE);
		builder.addHeader("operation", json.action);
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PutMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> trigger(SchedulerPathVariables path, @RequestBody SchedulerModel json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SCHEDULER, BusAction.MANAGE);
		builder.addHeader("operation", json.action);
		builder.addHeader("job", path.jobName);
		builder.addHeader("group", path.groupName);
		return frankApiService.callSyncGateway(builder);
	}

	// Database jobs

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PostMapping(value = "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createSchedule(ScheduleMultipartModel multipartBody) {
		String jobGroupName = RequestUtils.resolveRequiredProperty("group", multipartBody.group(), null);
		return createSchedule(jobGroupName, multipartBody);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@PutMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> updateSchedule(SchedulerPathVariables path,
											ScheduleMultipartModel multipartBody) {
		return createSchedule(path.groupName, path.jobName, multipartBody, true);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PostMapping(value = "/schedules/{groupName}/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createScheduleInJobGroup(SchedulerPathVariables path, ScheduleMultipartModel multipartBody) {
		return createSchedule(path.groupName, multipartBody);
	}

	private ResponseEntity<?> createSchedule(String groupName, ScheduleMultipartModel input) {
		String jobName = RequestUtils.resolveRequiredProperty("name", input.name(), null);
		return createSchedule(groupName, jobName, input, false);
	}

	protected ResponseEntity<?> createSchedule(String groupName, String jobName, ScheduleMultipartModel multipartBody, boolean overwrite) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SCHEDULER, BusAction.UPLOAD);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);

		builder.addHeader("cron", RequestUtils.resolveRequiredProperty("cron", multipartBody.cron(), ""));
		builder.addHeader("interval", RequestUtils.resolveRequiredProperty("interval", multipartBody.interval(), -1));

		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, RequestUtils.resolveRequiredProperty("adapter", multipartBody.adapter(), null));
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, RequestUtils.resolveRequiredProperty("receiver", multipartBody.receiver(), ""));
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, RequestUtils.resolveRequiredProperty(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, multipartBody.configuration(), ""));
		builder.addHeader("listener", RequestUtils.resolveRequiredProperty("listener", multipartBody.listener(), ""));

		builder.addHeader("persistent", RequestUtils.resolveRequiredProperty("persistent", multipartBody.persistent(), false));
		builder.addHeader("locker", RequestUtils.resolveRequiredProperty("locker", multipartBody.locker(), false));
		builder.addHeader("lockkey", RequestUtils.resolveRequiredProperty("lockkey", multipartBody.lockkey(), "lock4[" + jobName + "]"));

		builder.addHeader("message", RequestUtils.resolveRequiredProperty("message", multipartBody.message(), null));
		builder.addHeader("description", RequestUtils.resolveRequiredProperty("description", multipartBody.description(), null));

		if (overwrite) {
			builder.addHeader("overwrite", overwrite);
		}

		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@DeleteMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteSchedules(SchedulerPathVariables path) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SCHEDULER, BusAction.DELETE);
		builder.addHeader("job", path.jobName);
		builder.addHeader("group", path.groupName);
		return frankApiService.callSyncGateway(builder);
	}

	public record SchedulerPathVariables(String groupName, String jobName) {}

	public record SchedulerModel(String action) {}

	public record ScheduleMultipartModel(
			String name,
			String group,
			String cron,
			Integer interval,
			String adapter,
			String receiver,
			String configuration,
			String listener,
			String lockkey,
			String message,
			String description,
			Boolean persistent,
			Boolean locker) {
	}
}
