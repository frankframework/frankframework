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
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.TopicSelector;
import nl.nn.adapterframework.util.EnumUtils;

@BusAware("frank-management-bus")
public class HandleIbisManagerAction {
	private @Getter @Setter IbisManager ibisManager;

	@TopicSelector(BusTopic.IBISACTION)
	public void handleIbisAction(Message<?> message) {
		String actionName = BusMessageUtils.getHeader(message, "action");
		String configurationName = BusMessageUtils.getHeader(message, "configuration");
		String adapterName = BusMessageUtils.getHeader(message, "adapter");
		String receiverName = BusMessageUtils.getHeader(message, "receiver");
		String userPrincipalName = BusMessageUtils.getHeader(message, "issuedBy");
		boolean isAdmin = BusMessageUtils.getHeader(message, "isAdmin", false); //limits the use of a FULL_RELOAD

		IbisAction action = EnumUtils.parse(IbisAction.class, actionName);
		getIbisManager().handleAction(action, configurationName, adapterName, receiverName, userPrincipalName, isAdmin);
	}
}
