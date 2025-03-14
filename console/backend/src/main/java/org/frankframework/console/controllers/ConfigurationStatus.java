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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.Action;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@RestController
public class ConfigurationStatus {

	private final FrankApiService frankApiService;

	public ConfigurationStatus(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@Relation("adapter")
	@Description("view a list of all adapters, prefixed with the configuration name")
	@GetMapping(value = "/adapters", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAdapters(@RequestParam Map<String, String> params) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.addHeader("showPendingMsgCount", params.get("showPendingMsgCount"));
		builder.addHeader("expanded", params.get("expanded"));
		return frankApiService.callSyncGateway(builder);
	}

	@AllowAllIbisUserRoles
	@Relation("adapter")
	@Description("view an adapter receivers/pipes/messages")
	@GetMapping(value = "/configurations/{configuration}/adapters/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAdapter(@PathVariable("configuration") String configuration, @PathVariable("name") String name,
										@RequestParam Map<String, String> params) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.FIND);
		addConfigurationAndAdapterNameHeaders(configuration, name, builder);

		builder.addHeader("showPendingMsgCount", params.get("showPendingMsgCount"));
		builder.addHeader("expanded", params.get("expanded"));
		return frankApiService.callSyncGateway(builder);
	}

	@PermitAll
	@Relation("adapter")
	@Description("view an adapter health")
	@GetMapping(value = "/configurations/{configuration}/adapters/{name}/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAdapterHealth(@PathVariable("configuration") String configuration, @PathVariable("name") String name) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.HEALTH);
		addConfigurationAndAdapterNameHeaders(configuration, name, builder);

		return frankApiService.callSyncGateway(builder);
	}

	// Normally you don't use the PUT method on a collection...
	@SuppressWarnings("unchecked")
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("start/stop multiple adapters")
	@PutMapping(value = "/adapters", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateAdapters(@RequestBody UpdateAdaptersModel json) {
		ArrayList<String> adapters = new ArrayList<>();

		String value = json.action;
		Action action = getActionOrThrow(value);

		if (json.adapters != null) {
			adapters.addAll(json.adapters);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISACTION);
		builder.addHeader("action", action.name());
		if (adapters.isEmpty()) {
			addConfigurationAndAdapterNameHeaders("*ALL*", "*ALL*", builder);
			frankApiService.callAsyncGateway(builder);
		} else {
			for (String adapterNameWithPossibleConfigurationName : adapters) {
				int slash = adapterNameWithPossibleConfigurationName.indexOf("/");
				String adapterName;
				if (slash > -1) {
					adapterName = adapterNameWithPossibleConfigurationName.substring(slash + 1);
					String configurationName = adapterNameWithPossibleConfigurationName.substring(0, slash);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configurationName);
				} else {
					adapterName = adapterNameWithPossibleConfigurationName;
				}
				builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapterName);
				frankApiService.callAsyncGateway(builder);
			}
		}

		return ResponseEntity.status(HttpStatus.ACCEPTED).build(); //PUT defaults to no content
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("start/stop an adapter")
	@PutMapping(value = "/configurations/{configuration}/adapters/{adapter}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateAdapter(@PathVariable("configuration") String configuration, @PathVariable("adapter") String adapter, @RequestBody UpdateAdapterOrReceiverModel json) {
		String value = json.action;
		Action action = getActionOrThrow(value);

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISACTION);
		addHeaders(builder, configuration, adapter, action);
		frankApiService.callAsyncGateway(builder);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body("{\"status\":\"ok\"}");
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("start/stop an adapter receivers")
	@PutMapping(value = "/configurations/{configuration}/adapters/{adapter}/receivers/{receiver}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateReceiver(@PathVariable("configuration") String configuration, @PathVariable("adapter") String adapter,
											@PathVariable("receiver") String receiver, @RequestBody UpdateAdapterOrReceiverModel json) {

		String value = json.action;
		Action action = null;

		if (StringUtils.isNotEmpty(value)) {
			if (value.equals("stop")) {
				action = Action.STOPRECEIVER;
			} else if (value.equals("start")) {
				action = Action.STARTRECEIVER;
			} else if (value.equals("incthread")) {
				action = Action.INCTHREADS;
			} else if (value.equals("decthread")) {
				action = Action.DECTHREADS;
			}
		}

		if (action == null) {
			throw new ApiException("no or unknown action provided", HttpStatus.BAD_REQUEST);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISACTION);
		addHeaders(builder, configuration, adapter, action);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);

		frankApiService.callAsyncGateway(builder);

		return ResponseEntity.status(HttpStatus.ACCEPTED).body("{\"status\":\"ok\"}");
	}

	@AllowAllIbisUserRoles
	@Relation("adapter")
	@Description("view an adapter flow")
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapter}/flow")
	public ResponseEntity<?> getAdapterFlow(@PathVariable("configuration") String configuration, @PathVariable("adapter") String adapter) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.FLOW);
		addConfigurationAndAdapterNameHeaders(configuration, adapter, builder);
		return frankApiService.callSyncGateway(builder);
	}

	public record UpdateAdapterOrReceiverModel(String action) {}

	public record UpdateAdaptersModel(String action, List<String> adapters) {}

	private void addHeaders(RequestMessageBuilder builder, String configuration, String adapter, Action action) {
		if (action == null) {
			throw new ApiException("no or unknown action provided", HttpStatus.BAD_REQUEST);
		}
		builder.addHeader("action", action.name());
		addConfigurationAndAdapterNameHeaders(configuration, adapter, builder);
	}

	private Action getActionOrThrow(String value) {
		if (StringUtils.isNotEmpty(value)) {
			if ("stop".equals(value)) {
				return Action.STOPADAPTER;
			}
			if ("start".equals(value)) {
				return Action.STARTADAPTER;
			}
		}

		throw new ApiException("no or unknown action provided", HttpStatus.BAD_REQUEST);
	}

	private void addConfigurationAndAdapterNameHeaders(String configuration, String name, RequestMessageBuilder builder) {
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, name);
	}
}
