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

import org.apache.logging.log4j.Level;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

@RestController
public class UpdateLoggingConfig {

	private final FrankApiService frankApiService;

	public UpdateLoggingConfig(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@Relation("logging")
	@Description("view the application log configuration")
	@GetMapping(value = "/server/logging", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getLogConfiguration() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.LOG_CONFIGURATION, BusAction.GET));
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("logging")
	@Description("update the application log configuration")
	@PutMapping(value = "/server/logging", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateLogConfiguration(@RequestBody UpdateLogConfigurationModel json) {
		Level loglevel = Level.toLevel(json.loglevel, null);

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.LOG_CONFIGURATION, BusAction.MANAGE);
		builder.addHeader("logLevel", loglevel == null ? null : loglevel.name());
		builder.addHeader("logIntermediaryResults", json.logIntermediaryResults);
		builder.addHeader("maxMessageLength", json.maxMessageLength);
		builder.addHeader("enableDebugger", json.enableDebugger);
		return frankApiService.callAsyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("logging")
	@Description("view the log definitions, default loggers and their corresponding levels")
	@GetMapping(value = "/server/logging/settings", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getLogDefinitions(@RequestParam(value = "filter", required = false) String filter) {
		RequestMessageBuilder request = RequestMessageBuilder.create(BusTopic.LOG_DEFINITIONS, BusAction.GET);
		request.addHeader("filter", filter);
		return frankApiService.callSyncGateway(request);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("logging")
	@Description("create a new logger definition")
	@PostMapping(value = "/server/logging/settings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<?> createLogDefinition(CreateLogDefinitionMultipartModel multipartBody) {
		String logger = RequestUtils.resolveRequiredProperty("logger", multipartBody.logger(), null);
		String level = RequestUtils.resolveRequiredProperty("level", multipartBody.level(), null);

		RequestMessageBuilder request = RequestMessageBuilder.create(BusTopic.LOG_DEFINITIONS, BusAction.UPLOAD);
		request.addHeader("logPackage", logger);
		request.addHeader("level", level);
		return frankApiService.callSyncGateway(request);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("logging")
	@Description("update the loglevel of a specific logger")
	@PutMapping(value = "/server/logging/settings", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateLogDefinition(@RequestBody UpdateLogDefinitionModel json) {
		RequestMessageBuilder request = RequestMessageBuilder.create(BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);

		Level level = Level.toLevel(json.level, null);
		if (level != null) {
			request.addHeader("level", level.name());
		}
		request.addHeader("logPackage", json.logger);
		request.addHeader("reconfigure", json.reconfigure);

		return frankApiService.callSyncGateway(request);
	}

	public record UpdateLogConfigurationModel(
			String loglevel,
			Boolean logIntermediaryResults,
			Boolean enableDebugger,
			Integer maxMessageLength
	) {}

	public record CreateLogDefinitionMultipartModel(
		String logger,
		String level
	) {}

	public record UpdateLogDefinitionModel(
			String level,
			String logger,
			Boolean reconfigure
	) {}
}
