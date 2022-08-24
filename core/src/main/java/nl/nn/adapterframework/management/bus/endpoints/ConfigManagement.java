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

@BusAware("frank-management-bus")
public class ConfigManagement {
	private @Getter @Setter IbisManager ibisManager;

	@TopicSelector(BusTopic.CONFIGURATION)
	public Message getXMLConfiguration(Message<?> message) {
		boolean loadedConfiguration = BusMessageUtils.getHeader(message, "loaded", false);
		StringBuilder result = new StringBuilder();
		for (Configuration configuration : getIbisManager().getConfigurations()) {
			if (loadedConfiguration) {
				result.append(configuration.getLoadedConfiguration());
			} else {
				result.append(configuration.getOriginalConfiguration());
			}
		}
		return ResponseMessage.ok(result.toString());
	}
}
