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

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@RestController
@RequestMapping("/configurations/{configuration}/monitors")
public class Monitors {

	private final FrankApiService frankApiService;

	private static final String MONITOR_HEADER = "monitor";
	private static final String TRIGGER_HEADER = "trigger";

	public Monitors(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	private static void addDefaultHeaders(String configName, String monitorName, RequestMessageBuilder builder) {
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configName);
		builder.addHeader(MONITOR_HEADER, monitorName);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("view all available monitors")
	@GetMapping(value = {"", "/"})
	public ResponseEntity<?> getMonitors(@PathVariable("configuration") String configurationName,
										 @RequestParam(value = "xml", defaultValue = "false") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.addHeader("xml", showConfigXml);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("add a new monitor")
	@PostMapping(value = {"", "/"}, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> addMonitor(@PathVariable("configuration") String configurationName, @RequestBody CreateMonitorModel json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
		builder.setJsonPayload(json);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("get a specific monitor")
	@GetMapping(value = "/{monitorName}")
	public ResponseEntity<?> getMonitor(@PathVariable("configuration") String configurationName, @PathVariable("monitorName") String monitorName,
										@RequestParam(value = "xml", defaultValue = "false") boolean showConfigXml) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.GET);
		addDefaultHeaders(configurationName, monitorName, builder);
		builder.addHeader("xml", showConfigXml);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("update a specific monitor")
	@PutMapping(value = "/{monitorName}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateMonitor(@PathVariable("configuration") String configName,
										   @PathVariable(value = "monitorName", required = false) String monitorName, @RequestBody Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.MANAGE);
		addDefaultHeaders(configName, monitorName, builder);

		Object state = json.remove("action");
		if (state != null) {
			builder.addHeader("state", String.valueOf(state));
		}
		builder.setJsonPayload(json);

		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("delete a specific monitor")
	@DeleteMapping(value = "/{monitorName}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteMonitor(@PathVariable("configuration") String configurationName,
										   @PathVariable(value = "monitorName", required = false) String monitorName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.DELETE);
		addDefaultHeaders(configurationName, monitorName, builder);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("view specific monitor")
	@GetMapping(value = "/{monitorName}/triggers", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getTriggers(@PathVariable("configuration") String configurationName,
										 @PathVariable(value = "monitorName", required = false) String monitorName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.GET);
		addDefaultHeaders(configurationName, monitorName, builder);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("update a specific monitors triggers")
	@PostMapping(value = "/{monitorName}/triggers", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> addTrigger(@PathVariable("configuration") String configName,
										@PathVariable(value = "monitorName", required = false) String monitorName, @RequestBody Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.UPLOAD);
		addDefaultHeaders(configName, monitorName, builder);
		builder.setJsonPayload(json);

		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("view all triggers for a specific monitor")
	@GetMapping(value = "/{monitorName}/triggers/{trigger}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getTrigger(@PathVariable("configuration") String configurationName,
										@PathVariable(value = "monitorName", required = false) String monitorName,
										@PathVariable(value = "trigger", required = false) Integer id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.GET);
		addDefaultHeaders(configurationName, monitorName, builder);
		builder.addHeader(TRIGGER_HEADER, id);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("update a specific monitor triggers")
	@PutMapping(value = "/{monitorName}/triggers/{trigger}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateTrigger(@PathVariable("configuration") String configName,
										   @PathVariable("monitorName") String monitorName, @PathVariable("trigger") int index,
										   @RequestBody Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.MANAGE);
		addDefaultHeaders(configName, monitorName, builder);
		builder.addHeader(TRIGGER_HEADER, index);
		builder.setJsonPayload(json);

		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("monitoring")
	@Description("delete a specific monitor trigger")
	@DeleteMapping(value = "/{monitorName}/triggers/{trigger}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> deleteTrigger(@PathVariable("configuration") String configurationName,
										   @PathVariable("monitorName") String monitorName, @PathVariable("trigger") int id) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MONITORING, BusAction.DELETE);
		addDefaultHeaders(configurationName, monitorName, builder);
		builder.addHeader(TRIGGER_HEADER, id);
		return frankApiService.callSyncGateway(builder);
	}

	public record CreateMonitorModel(String name) {}
}
