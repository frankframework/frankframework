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
package nl.nn.adapterframework.management.bus.endpoints;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.management.bus.ActionSelector;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.JsonResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.receivers.ServiceDispatcher;

@BusAware("frank-management-bus")
public class ServiceListener extends BusEndpointBase {

	@TopicSelector(BusTopic.SERVICE_LISTENER)
	@ActionSelector(BusAction.GET)
	public Message<String> getServiceListeners(Message<?> message) {
		Map<String, Object> returnData = new HashMap<>();

		Set<String> services = ServiceDispatcher.getInstance().getRegisteredListenerNames();
		returnData.put("services", services);

		return new JsonResponseMessage(returnData);
	}

	@TopicSelector(BusTopic.SERVICE_LISTENER)
	@ActionSelector(BusAction.UPLOAD)
	public Message<String> postServiceListeners(Message<?> message) {
		String serviceName = BusMessageUtils.getHeader(message, "service");
		if(StringUtils.isEmpty(serviceName)) {
			throw new BusException("service name not provided");
		}

		if(!ServiceDispatcher.getInstance().isRegisteredServiceListener(serviceName)) {
			throw new BusException("ServiceListener not found");
		}

		nl.nn.adapterframework.stream.Message payload = nl.nn.adapterframework.stream.Message.asMessage(message.getPayload());
		nl.nn.adapterframework.stream.Message dispatchResult;
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

		return new JsonResponseMessage(result);
	}
}
