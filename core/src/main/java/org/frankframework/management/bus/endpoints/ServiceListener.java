/*
   Copyright 2022 WeAreFrank!

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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.JsonMessage;
import org.springframework.messaging.Message;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineSession;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.receivers.ServiceDispatcher;

@BusAware("frank-management-bus")
public class ServiceListener extends BusEndpointBase {

	@TopicSelector(BusTopic.SERVICE_LISTENER)
	@ActionSelector(BusAction.GET)
	@RolesAllowed({ "IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester" })
	public Message<String> getServiceListeners(Message<?> message) {
		Map<String, Object> returnData = new HashMap<>();

		Set<String> services = ServiceDispatcher.getInstance().getRegisteredListenerNames();
		returnData.put("services", services);

		return new JsonMessage(returnData);
	}

	@TopicSelector(BusTopic.SERVICE_LISTENER)
	@ActionSelector(BusAction.UPLOAD)
	@RolesAllowed("IbisTester")
	public Message<String> postServiceListeners(Message<?> message) {
		String serviceName = BusMessageUtils.getHeader(message, "service");
		if(StringUtils.isEmpty(serviceName)) {
			throw new BusException("service name not provided");
		}

		if(!ServiceDispatcher.getInstance().isRegisteredServiceListener(serviceName)) {
			throw new BusException("ServiceListener not found");
		}

		org.frankframework.stream.Message payload = org.frankframework.stream.Message.asMessage(message.getPayload());
		org.frankframework.stream.Message dispatchResult;
		try (PipeLineSession session = new PipeLineSession()) {
			dispatchResult = ServiceDispatcher.getInstance().dispatchRequest(serviceName, payload, session);
		} catch (ListenerException e) {
			throw new BusException("Exception executing service ["+serviceName+"]", e);
		}

		Map<String, String> result = new HashMap<>();
		result.put("state", ExitState.SUCCESS.name());
		try {
			result.put("result", dispatchResult.asString());
		} catch (IOException e) {
			throw new BusException("Error converting result to string: " + e.getMessage(), e);
		}

		return new JsonMessage(result);
	}
}
