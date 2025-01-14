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
package org.frankframework.management.bus.endpoints;

import jakarta.annotation.security.RolesAllowed;

import org.springframework.messaging.Message;

import org.frankframework.management.Action;
import org.frankframework.management.bus.BusAware;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.TopicSelector;

@BusAware("frank-management-bus")
public class HandleIbisManagerAction extends BusEndpointBase {

	@TopicSelector(BusTopic.IBISACTION)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public void handleIbisAction(Message<?> message) {
		Action action = BusMessageUtils.getEnumHeader(message, "action", Action.class);
		String configurationName = BusMessageUtils.getHeader(message, "configuration", BusMessageUtils.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, "adapter");
		String receiverName = BusMessageUtils.getHeader(message, "receiver");
		String userPrincipalName = BusMessageUtils.getUserPrincipalName();
		boolean isAdmin = BusMessageUtils.hasAnyRole("IbisAdmin", "IbisTester"); //limits the use of a FULL_RELOAD

		if(action == null) {
			throw new BusException("no (valid) action specified");
		}
		getIbisManager().handleAction(action, configurationName, adapterName, receiverName, userPrincipalName, isAdmin);
	}
}
