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

import javax.annotation.security.RolesAllowed;

import org.springframework.messaging.Message;
import org.springframework.security.core.userdetails.UserDetails;

import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.management.bus.BusAware;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.TopicSelector;

@BusAware("frank-management-bus")
public class HandleIbisManagerAction extends BusEndpointBase {

	@TopicSelector(BusTopic.IBISACTION)
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	public void handleIbisAction(Message<?> message) {
		IbisAction action = BusMessageUtils.getEnumHeader(message, "action", IbisAction.class);
		String configurationName = BusMessageUtils.getHeader(message, "configuration", IbisManager.ALL_CONFIGS_KEY);
		String adapterName = BusMessageUtils.getHeader(message, "adapter");
		String receiverName = BusMessageUtils.getHeader(message, "receiver");
		String userPrincipalName = null;
		UserDetails user = BusMessageUtils.getUserDetails();
		boolean isAdmin = false;
		if(user != null) {
			userPrincipalName = user.getUsername();
			isAdmin = BusMessageUtils.hasAnyRole("IbisAdmin", "IbisTester"); //limits the use of a FULL_RELOAD
		}

		getIbisManager().handleAction(action, configurationName, adapterName, receiverName, userPrincipalName, isAdmin);
	}
}
