/*
   Copyright 2022 - 2024 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.receivers.Receiver;
import org.frankframework.util.RunState;

@BusAware("frank-management-bus")
public class HealthCheck extends BusEndpointBase {

	@TopicSelector(BusTopic.HEALTH)
	@PermitAll
	public Message<String> getHealth(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY);
		if(StringUtils.isNotEmpty(configurationName)) {
			Configuration configuration = getConfigurationByName(configurationName);

			String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
			if(StringUtils.isNotEmpty(adapterName)) {
				Adapter adapter = configuration.getRegisteredAdapter(adapterName);

				if(adapter == null) {
					throw new BusException("adapter ["+adapterName+"] does not exist");
				}

				return getAdapterHealth(adapter);
			}
			return getConfigurationHealth(configuration);
		}

		return getApplicationHealth();
	}

	private Message<String> getApplicationHealth() {
		Map<String, Object> response = new HashMap<>();

		List<String> errors = new ArrayList<>();

		for(Configuration config : getIbisManager().getConfigurations()) {
			RunState state = config.getState();
			if(state != RunState.STARTED) {
				if(config.getConfigurationException() != null) {
					errors.add("configuration["+config.getName()+"] is in state[ERROR]");
				} else {
					errors.add("configuration["+config.getName()+"] is in state["+state+"]");
				}
			}
		}

		Status status = Response.Status.OK;
		if(!errors.isEmpty()) {
			response.put("errors", errors);
			status = Response.Status.SERVICE_UNAVAILABLE;
		}
		response.put("status", status);

		JsonMessage responseMessage = new JsonMessage(response);
		responseMessage.setStatus(status.getStatusCode());
		return responseMessage;
	}

	/**
	 * Returns the status of a configuration. If an Adapter is not in state STARTED it is flagged as NOT-OK.
	 * @param configuration The name of the Configuration to get health info from
	 */
	private Message<String> getConfigurationHealth(Configuration configuration) {
		if(!configuration.isActive()) {
			throw new BusException("configuration not active", configuration.getConfigurationException());
		}

		Map<String, Object> response = new HashMap<>();
		Map<RunState, Integer> stateCount = new EnumMap<>(RunState.class);
		List<String> errors = new ArrayList<>();

		for (Adapter adapter : configuration.getRegisteredAdapters()) {
			RunState state = adapter.getRunState(); //Let's not make it difficult for ourselves and only use STARTED/ERROR enums

			if(state==RunState.STARTED) {
				for (Receiver<?> receiver: adapter.getReceivers()) {
					RunState rState = receiver.getRunState();

					if(rState!=RunState.STARTED) {
						errors.add("receiver["+receiver.getName()+"] of adapter["+adapter.getName()+"] is in state["+rState.toString()+"]");
						state = RunState.ERROR;
					}
				}
			}
			else {
				errors.add("adapter["+adapter.getName()+"] is in state["+state.toString()+"]");
				state = RunState.ERROR;
			}

			int count;
			if(stateCount.containsKey(state))
				count = stateCount.get(state);
			else
				count = 0;

			stateCount.put(state, ++count);
		}

		Status status = Status.OK;
		if(stateCount.containsKey(RunState.ERROR))
			status = Status.SERVICE_UNAVAILABLE;

		if(!errors.isEmpty())
			response.put("errors", errors);
		response.put("status", status);

		JsonMessage responseMessage = new JsonMessage(response);
		responseMessage.setStatus(status.getStatusCode());
		return responseMessage;
	}

	/**
	 * Returns the status of an {@link Adapter}. If the adapter is not in state STARTED it is flagged as NOT-OK.
	 * @param adapter The name of the Adapter to get health info from
	 */
	private Message<String> getAdapterHealth(Adapter adapter) {
		Map<String, Object> response = new HashMap<>();
		List<String> errors = new ArrayList<>();

		RunState state = adapter.getRunState(); //Let's not make it difficult for ourselves and only use STARTED/ERROR enums

		if(state==RunState.STARTED) {
			for (Receiver<?> receiver: adapter.getReceivers()) {
				RunState rState = receiver.getRunState();

				if(rState!=RunState.STARTED) {
					errors.add("receiver["+receiver.getName()+"] of adapter["+adapter.getName()+"] is in state["+rState.toString()+"]");
					state = RunState.ERROR;
				}
			}
		} else {
			errors.add("adapter["+adapter.getName()+"] is in state["+state.toString()+"]");
			state = RunState.ERROR;
		}

		Status status = Response.Status.OK;
		if(state==RunState.ERROR) {
			status = Response.Status.SERVICE_UNAVAILABLE;
		}
		if(!errors.isEmpty())
			response.put("errors", errors);
		response.put("status", status);

		JsonMessage responseMessage = new JsonMessage(response);
		responseMessage.setStatus(status.getStatusCode());
		return responseMessage;
	}
}
