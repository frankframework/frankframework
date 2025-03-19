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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.management.Action;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.HttpUtils;

@RestController
public class Configurations {

	private final FrankApiService frankApiService;

	private static final String BUS_HEADER_VERSION = "version";

	public Configurations(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@Relation("application")
	@Description("view all the loaded/original configurations")
	@GetMapping(value = "/configurations", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<?> getConfigurationXML(ConfigurationParameters params) throws ApiException {
		boolean loaded = params.loadedConfiguration;
		String flow = params.flow;

		if (StringUtils.isNotEmpty(flow)) {
			RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.FLOW);
			return frankApiService.callSyncGateway(builder);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.GET);
		if (loaded) {
			builder.addHeader("loaded", true);
		}
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisAdmin", "IbisTester"})
	@Relation("application")
	@Description("update the entire application using an action")
	@PutMapping(value = "/configurations", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> fullAction(@RequestBody ActionModel json) throws ApiException {
		List<String> configurations = new ArrayList<>();
		Action action = getActionOrThrow(json.action, true);

		String[] configurationsList = json.configurations;
		if (configurationsList != null) {
			try {
				configurations.addAll(List.of(configurationsList));
			} catch (Exception e) {
				throw new ApiException(e);
			}
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISACTION);
		builder.addHeader("action", action.name());
		if (configurations.isEmpty()) {
			builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, BusMessageUtils.ALL_CONFIGS_KEY);
			frankApiService.callAsyncGateway(builder);
		} else {
			for (String configurationName : configurations) {
				builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
				builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, BusMessageUtils.ALL_CONFIGS_KEY);
				frankApiService.callAsyncGateway(builder);
			}
		}

		return ResponseEntity.status(HttpStatus.ACCEPTED).build();
	}

	@AllowAllIbisUserRoles
	@Relation("configuration")
	@Description("view individual loaded/original configuration")
	@GetMapping(value = "/configurations/{configuration}", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<?> getConfigurationByName(ConfigurationPathVariables path,
													ConfigurationParameters params) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.GET);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);

		if (params.loadedConfiguration) {
			builder.addHeader("loaded", true);
		}

		return frankApiService.callSyncGateway(builder);
	}

	@PermitAll
	@Relation("configuration")
	@Description("view configuration health")
	@GetMapping(value = "/configurations/{configuration}/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getConfigurationHealth(ConfigurationPathVariables path) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.HEALTH);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("configuration")
	@Description("view configuration flow diagram")
	@GetMapping(value = "/configurations/{configuration}/flow")
	public ResponseEntity<?> getConfigurationFlow(ConfigurationPathVariables path) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.FLOW);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("configuration")
	@Description("update a specific configuration using an action")
	@PutMapping(value = "/configurations/{configuration}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateConfiguration(ConfigurationPathVariables path, @RequestBody UpdateConfigurationModel json) throws ApiException {
		Action action = getActionOrThrow(json.action, false);

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISACTION);
		builder.addHeader("action", action.name());
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		frankApiService.callAsyncGateway(builder);
		return ResponseEntity.status(HttpStatus.ACCEPTED).body("{\"status\":\"ok\"}");
	}

	@AllowAllIbisUserRoles
	@Relation("configuration")
	@Description("view a list of all known configuration versions")
	@GetMapping(value = "/configurations/{configuration}/versions", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getConfigurationDetailsByName(ConfigurationPathVariables path,
														   ConfigurationParameters params) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.FIND);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, params.datasourceName);
		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	@Relation("configuration")
	@Description("change the active configuration version, and optionally schedule or load it directly")
	@PutMapping(value = "/configurations/{configuration}/versions/{version}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> manageConfiguration(ConfigurationPathVariables path,
												 ConfigurationParameters params,
												 @RequestBody ManageConfigurationModel json) throws ApiException {

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.MANAGE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BUS_HEADER_VERSION, HttpUtils.urlDecode(path.version));

		if (json.activate != null) {
			builder.addHeader("activate", json.activate);
		} else if (json.autoreload != null) {
			builder.addHeader("autoreload", json.autoreload);
		}

		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, params.datasourceName);

		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	@Relation("configuration")
	@Description("upload a new configuration versions")
	@PostMapping(value = "/configurations", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> uploadConfiguration(UploadConfigurationModel multipartBody) throws ApiException {
		String datasource = RequestUtils.resolveRequiredProperty("datasource", multipartBody.datasource, "");
		boolean multipleConfigs = RequestUtils.resolveRequiredProperty("multiple_configs", multipartBody.multiple_configs, false);
		boolean activateConfig = RequestUtils.resolveRequiredProperty("activate_config", multipartBody.activate_config, true);
		boolean automaticReload = RequestUtils.resolveRequiredProperty("automatic_reload", multipartBody.automatic_reload, false);

		MultipartFile filePart = multipartBody.file;
		InputStream file;
		try {
			file = filePart.getInputStream();
		} catch (IOException e) {
			throw new ApiException(e);
		}

		String user = RequestUtils.resolveRequiredProperty("user", multipartBody.user, "");
		if (StringUtils.isEmpty(user)) {
			user = BusMessageUtils.getUserPrincipalName();
		}

		String fileNameOrPath = filePart.getOriginalFilename();
		String fileName = Paths.get(fileNameOrPath).getFileName().toString();

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.UPLOAD);
		builder.setPayload(file);
		builder.addHeader("filename", fileName);
		builder.addHeader("multiple_configs", multipleConfigs);
		builder.addHeader("activate_config", activateConfig);
		builder.addHeader("automatic_reload", automaticReload);
		builder.addHeader("user", user);

		if (StringUtils.isNotEmpty(datasource)) {
			builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		}

		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("configuration")
	@Description("download a specific configuration version")
	@GetMapping(value = "/configurations/{configuration}/versions/{version}/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	public ResponseEntity<?> downloadConfiguration(ConfigurationPathVariables path,
												   ConfigurationParameters params) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.DOWNLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BUS_HEADER_VERSION, path.version);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, params.datasourceName);

		return frankApiService.callSyncGateway(builder);
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("configuration")
	@Description("delete a specific configuration")
	@DeleteMapping(value = "/configurations/{configuration}/versions/{version}")
	public ResponseEntity<?> deleteConfiguration(ConfigurationPathVariables path,
												 ConfigurationParameters params) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.DELETE);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, path.configuration);
		builder.addHeader(BUS_HEADER_VERSION, path.version);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, params.datasourceName);

		return frankApiService.callAsyncGateway(builder);
	}

	public record ConfigurationPathVariables(String configuration, String version) {}

	public record ConfigurationParameters(Boolean loadedConfiguration, String flow, String datasourceName) {}

	public record ActionModel(String action, String[] configurations) {}

	public record UpdateConfigurationModel(String action) {}

	public record ManageConfigurationModel(
			Boolean activate,
			Boolean autoreload
	) {}

	public record UploadConfigurationModel(
			String datasource,
			String user,
			boolean multiple_configs,
			boolean activate_config,
			boolean automatic_reload,
			MultipartFile file) {
	}

	private Action getActionOrThrow(String value, boolean fullAction) {
		if (StringUtils.isNotEmpty(value)) {
			switch (value) {
				case "stop" -> {
					return Action.STOPADAPTER;
				}
				case "start" -> {
					return Action.STARTADAPTER;
				}
				case "reload" -> {
					return Action.RELOAD;
				}
				case "fullreload" -> {
					if(fullAction) return Action.FULLRELOAD;
				}
			}
		}

		throw new ApiException("no or unknown action provided", HttpStatus.BAD_REQUEST);
	}
}
