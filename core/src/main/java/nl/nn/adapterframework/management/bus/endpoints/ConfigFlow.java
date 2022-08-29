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
import java.io.InputStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.ResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

@BusAware("frank-management-bus")
public class ConfigFlow {
	private @Getter @Setter IbisManager ibisManager;
	private @Setter FlowDiagramManager flowDiagramManager;

	@TopicSelector(BusTopic.FLOW)
	public Message<?> getFlow(Message<?> message) throws IOException {
		String configurationName = BusMessageUtils.getHeader(message, "configuration");
		if(StringUtils.isNotEmpty(configurationName)) {
			return getConfigurationFlow(configurationName);
		}
		return getApplicationFlow();
	}

	public Message<?> getApplicationFlow() throws IOException {
		InputStream configFlow = flowDiagramManager.get(getIbisManager().getConfigurations());
		if(configFlow != null) {
			return ResponseMessage.ok(configFlow, flowDiagramManager.getMediaType());
		}

		return ResponseMessage.noContent();
	}

	public Message<?> getConfigurationFlow(String configurationName) throws IOException {
		Configuration configuration = getIbisManager().getConfiguration(configurationName);

		if(configuration == null) {
			throw new IllegalStateException("Configuration not found!"); //should be wrapped in a message?
		}

		InputStream flow = flowDiagramManager.get(configuration);
		if(flow != null) {
			return ResponseMessage.ok(flow, flowDiagramManager.getMediaType());
		}

		return ResponseMessage.noContent();
	}
}
