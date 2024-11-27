/*
   Copyright 2016-2024 WeAreFrank!

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
import jakarta.annotation.security.RolesAllowed;
import org.frankframework.core.Adapter;
import org.frankframework.management.bus.ActionSelector;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;
import org.frankframework.management.bus.message.EmptyMessage;
import org.frankframework.management.bus.message.JsonMessage;
import org.frankframework.metrics.LocalStatisticsRegistry;
import org.springframework.messaging.Message;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

@BusAware("frank-management-bus")
@TopicSelector(BusTopic.ADAPTER)
public class AdapterStatistics extends BusEndpointBase {
	private LocalStatisticsRegistry localRegistry;

	@Override
	protected void doAfterPropertiesSet() {
		MeterRegistry metersRegistry = getBean("meterRegistry", MeterRegistry.class);
		if (metersRegistry instanceof CompositeMeterRegistry compositeMeterRegistry) {
			for(MeterRegistry meterRegistry: compositeMeterRegistry.getRegistries()) {
				if (meterRegistry instanceof LocalStatisticsRegistry registry) {
					localRegistry = registry;
				}
			}
		}
	}

	@ActionSelector(BusAction.STATUS)
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public Message<String> getStatistics(Message<?> message) {
		String configurationName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, BusMessageUtils.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, BusMessageUtils.HEADER_ADAPTER_NAME_KEY);
		Adapter adapter = getAdapterByName(configurationName, adapterName);

		if(localRegistry != null) {
			return new JsonMessage(localRegistry.scrape(configurationName, adapter));
		}
		return new EmptyMessage(404);
	}
}
