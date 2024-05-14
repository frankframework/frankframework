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
package org.frankframework.management.web.spring;

import lombok.Getter;
import lombok.Setter;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.web.Relation;
import org.frankframework.util.RequestUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.RolesAllowed;

import java.util.Map;

@RestController
public class Scheduler extends FrankApiBase {

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@GetMapping(value = "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getSchedules() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.GET));
	}

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@GetMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getSchedule(@PathVariable("jobName") String jobName, @PathVariable("groupName") String groupName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.FIND);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PutMapping(value = "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateScheduler(Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.MANAGE);
		builder.addHeader("operation", RequestUtils.getValue(json, "action"));
		return callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PutMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> trigger(@PathVariable("jobName") String jobName, @PathVariable("groupName") String groupName, Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.MANAGE);
		builder.addHeader("operation", RequestUtils.getValue(json, "action"));
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}

	// Database jobs

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PostMapping(value = "/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createSchedule(ScheduleMultipartBody multipartBody) {
		String jobGroupName = RequestUtils.resolveRequiredProperty("group", multipartBody.getGroup(), null);
		return createSchedule(jobGroupName, multipartBody);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@PutMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> updateSchedule(
			@PathVariable("groupName") String groupName,
			@PathVariable("jobName") String jobName,
			ScheduleMultipartBody multipartBody
	) {
		return createSchedule(this, groupName, jobName, multipartBody, true);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@PostMapping(value = "/schedules/{groupName}/jobs", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createScheduleInJobGroup(@PathVariable("groupName") String groupName, ScheduleMultipartBody multipartBody) {
		return createSchedule(groupName, multipartBody);
	}

	private ResponseEntity<?> createSchedule(String groupName, ScheduleMultipartBody input) {
		String jobName = RequestUtils.resolveRequiredProperty("name", input.getName(), null);
		return createSchedule(this, groupName, jobName, input, false);
	}

	protected static ResponseEntity<?> createSchedule(
			FrankApiBase base,
			String groupName,
			String jobName,
			ScheduleMultipartBody multipartBody,
			boolean overwrite
	) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(base, BusTopic.SCHEDULER, BusAction.UPLOAD);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);

		builder.addHeader("cron", RequestUtils.resolveRequiredProperty("cron", multipartBody.getCron(), ""));
		builder.addHeader("interval", RequestUtils.resolveRequiredProperty("interval", multipartBody.getInterval(), -1));

		builder.addHeader("adapter", RequestUtils.resolveRequiredProperty("adapter", multipartBody.getAdapter(), null));
		builder.addHeader("receiver", RequestUtils.resolveRequiredProperty("receiver", multipartBody.getReceiver(), ""));
		builder.addHeader("configuration", RequestUtils.resolveRequiredProperty(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, multipartBody.getConfiguration(), ""));
		builder.addHeader("listener", RequestUtils.resolveRequiredProperty("listener", multipartBody.getListener(), ""));

		builder.addHeader("persistent", RequestUtils.resolveRequiredProperty("persistent", multipartBody.isPersistent(), false));
		builder.addHeader("locker", RequestUtils.resolveRequiredProperty("locker", multipartBody.isLocker(), false));
		builder.addHeader("lockkey", RequestUtils.resolveRequiredProperty("lockkey", multipartBody.getLockkey(), "lock4[" + jobName + "]"));

		builder.addHeader("message", RequestUtils.resolveRequiredProperty("message", multipartBody.getMessage(), null));

		builder.addHeader("description", RequestUtils.resolveRequiredProperty("description", multipartBody.getDescription(), null));
		if (overwrite) {
			builder.addHeader("overwrite", overwrite);
		}

		return base.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("schedules")
	@DeleteMapping(value = "/schedules/{groupName}/jobs/{jobName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteSchedules(@PathVariable("jobName") String jobName, @PathVariable("groupName") String groupName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.SCHEDULER, BusAction.DELETE);
		builder.addHeader("job", jobName);
		builder.addHeader("group", groupName);
		return callSyncGateway(builder);
	}

	@Getter
	@Setter
	public static class ScheduleMultipartBody {
		private String name;
		private String group;

		private String cron;
		private Integer interval;
		private String adapter;
		private String receiver;
		private String configuration;
		private String listener;
		private String lockkey;
		private String message;
		private String description;
		private boolean persistent;
		private boolean locker;
	}

}
