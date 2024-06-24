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
package org.frankframework.management.web;

import java.util.ArrayList;
import java.util.Map;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.IbisAction;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.RequestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigurationStatus extends FrankApiBase {

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("view a list of all adapters")
	@GetMapping(value = "/adapters", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAdapters(@RequestParam(value = "expanded", required = false) String expanded, @RequestParam(value = "showPendingMsgCount", required = false) boolean showPendingMsgCount) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.GET);
		builder.addHeader("showPendingMsgCount", showPendingMsgCount);
		builder.addHeader("expanded", expanded);
		return callSyncGateway(builder);
	}

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("view an adapter receivers/pipes/messages")
	@GetMapping(value = "/configurations/{configuration}/adapters/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAdapter(@PathVariable("configuration") String configuration, @PathVariable("name") String name, @RequestParam(value = "expanded", required = false) String expanded, @RequestParam(value = "showPendingMsgCount", required = false) boolean showPendingMsgCount) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.FIND);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, name);

		builder.addHeader("showPendingMsgCount", showPendingMsgCount);
		builder.addHeader("expanded", expanded);
		return callSyncGateway(builder);
	}

	@PermitAll
	@Relation("adapter")
	@Description("view an adapter health")
	@GetMapping(value = "/configurations/{configuration}/adapters/{name}/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAdapterHealth(@PathVariable("configuration") String configuration, @PathVariable("name") String name) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.HEALTH);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, name);

		return callSyncGateway(builder);
	}

	//Normally you don't use the PUT method on a collection...
	@SuppressWarnings("unchecked")
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("start/stop multiple adapters")
	@PutMapping(value = "/adapters", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateAdapters(@RequestBody Map<String, Object> json) {

		IbisAction action = null;
		ArrayList<String> adapters = new ArrayList<>();

		String value = RequestUtils.getValue(json, "action");
		if (StringUtils.isNotEmpty(value)) {
			if ("stop".equals(value)) {action = IbisAction.STOPADAPTER;}
			if ("start".equals(value)) {action = IbisAction.STARTADAPTER;}
		}
		if (action == null) {
			throw new ApiException("no or unknown action provided", HttpStatus.BAD_REQUEST);
		}

		Object adapterList = json.get("adapters");
		if (adapterList != null) {
			try {
				adapters.addAll((ArrayList<String>) adapterList);
			} catch (Exception e) {
				throw new ApiException(e);
			}
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
		builder.addHeader("action", action.name());
		if (adapters.isEmpty()) {
			builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, "*ALL*");
			builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, "*ALL*");
			callAsyncGateway(builder);
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
				callAsyncGateway(builder);
			}
		}

		return ResponseEntity.status(HttpStatus.ACCEPTED).build(); //PUT defaults to no content
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("start/stop an adapter")
	@PutMapping(value = "/configurations/{configuration}/adapters/{adapter}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateAdapter(@PathVariable("configuration") String configuration, @PathVariable("adapter") String adapter, @RequestBody Map<String, Object> json) {
		Object value = json.get("action");
		if (value instanceof String) {
			IbisAction action = null;
			if (value.equals("stop")) {action = IbisAction.STOPADAPTER;}
			if (value.equals("start")) {action = IbisAction.STARTADAPTER;}
			if (action == null) {
				throw new ApiException("no or unknown action provided", HttpStatus.BAD_REQUEST);
			}

			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
			builder.addHeader("action", action.name());
			builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
			builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
			callAsyncGateway(builder);
			return ResponseEntity.status(HttpStatus.ACCEPTED).body("{\"status\":\"ok\"}");
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("start/stop an adapter receivers")
	@PutMapping(value = "/configurations/{configuration}/adapters/{adapter}/receivers/{receiver}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updateReceiver(@PathVariable("configuration") String configuration, @PathVariable("adapter") String adapter, @PathVariable("receiver") String receiver, @RequestBody Map<String, Object> json) {
		Object value = json.get("action");
		if (value instanceof String) {
			IbisAction action = null;
			if (value.equals("stop")) {action = IbisAction.STOPRECEIVER;} else if (value.equals("start")) {
				action = IbisAction.STARTRECEIVER;
			} else if (value.equals("incthread")) {
				action = IbisAction.INCTHREADS;
			} else if (value.equals("decthread")) {
				action = IbisAction.DECTHREADS;
			}
			if (action == null) {
				throw new ApiException("no or unknown action provided", HttpStatus.BAD_REQUEST);
			}

			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
			builder.addHeader("action", action.name());
			builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
			builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
			builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);
			callAsyncGateway(builder);
			return ResponseEntity.status(HttpStatus.ACCEPTED).body("{\"status\":\"ok\"}");
		}

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
	}

	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Relation("adapter")
	@Description("view an adapter flow")
	@GetMapping(value = "/configurations/{configuration}/adapters/{adapter}/flow")
	public ResponseEntity<?> getAdapterFlow(@PathVariable("configuration") String configuration, @PathVariable("adapter") String adapter) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FLOW);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		return callSyncGateway(builder);
	}

}
