/*
   Copyright 2022-2023 WeAreFrank!

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

import lombok.Setter;
import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.management.bus.BinaryResponseMessage;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.EmptyResponseMessage;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

@BusAware("frank-management-bus")
public class ConfigFlow extends BusEndpointBase {
	private @Setter FlowDiagramManager flowDiagramManager;

	@TopicSelector(BusTopic.FLOW)
	public Message<?> getFlowDiagram(Message<?> message) throws IOException {
		InputStream flow = getFlow(message);
		if(flow != null) {
			return new BinaryResponseMessage(flow, flowDiagramManager.getMediaType());
		}

		return EmptyResponseMessage.noContent(); //No flow file present
	}

	private InputStream getFlow(Message<?> message) throws IOException {
		String configurationName = BusMessageUtils.getHeader(message, "configuration");
		if(StringUtils.isNotEmpty(configurationName)) {
			Configuration configuration = getIbisManager().getConfiguration(configurationName);
			if (configuration==null) {
				throw new IOException("configuration ["+configurationName+"] not found");
			}
			String adapterName = BusMessageUtils.getHeader(message, "adapter");
			if(StringUtils.isNotEmpty(adapterName)) {
				return getAdapterFlow(configuration, adapterName);
			}
			return getConfigurationFlow(configuration);
		}
		return getApplicationFlow();
	}

	private InputStream getApplicationFlow() throws IOException {
		return flowDiagramManager.get(getIbisManager().getConfigurations());
	}

	private InputStream getConfigurationFlow(Configuration configuration) throws IOException {
		return flowDiagramManager.get(configuration);
	}

	private InputStream getAdapterFlow(Configuration configuration, String adapterName) throws IOException {
		Adapter adapter = configuration.getRegisteredAdapter(adapterName);

		if(adapter == null) {
			throw new BusException("Adapter not found!");
		}

		return flowDiagramManager.get(adapter);
	}
}
