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

import jakarta.annotation.security.PermitAll;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/server")
public class ServerDetails {

	private final FrankApiService frankApiService;

	public ServerDetails(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerInformation() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.GET));
	}

	@AllowAllIbisUserRoles
	@Relation("configuration")
	@GetMapping(value = "/configurations", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAllConfigurations() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.FIND));
	}

	@AllowAllIbisUserRoles
	@Relation("configuration")
	@Description("download all active configurations")
	@GetMapping(value = "/configurations/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> downloadActiveConfigurations(@RequestParam(value = "dataSourceName", required = false) String dataSourceName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.DOWNLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, BusMessageUtils.ALL_CONFIGS_KEY);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, dataSourceName);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Description("view all configuration warnings")
	@GetMapping(value = "/warnings", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerConfiguration() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.WARNINGS), true);
	}

	@PermitAll
	@GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getFrankHealth() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.HEALTH));
	}
}
