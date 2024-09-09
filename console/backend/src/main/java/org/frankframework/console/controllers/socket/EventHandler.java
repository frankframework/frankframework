/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.console.controllers.socket;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nullable;

import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway.ClusterMember;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventHandler extends FrankApiWebSocketBase {

	@Scheduled(fixedDelay = 60, timeUnit = TimeUnit.SECONDS, initialDelay = 60)
	public void serverWarnings() {
		propagateAuthenticationContext("server-warnings");
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.WARNINGS);

		List<ClusterMember> members = getClusterMembers();
		if(members.isEmpty()) {
			convertAndSend(builder, "/event/server-warnings", null);
		} else {
			for (ClusterMember clusterMember : members) {
				UUID id = clusterMember.getId();
				convertAndSend(builder, "/event/"+id.toString()+"/server-warnings", id);
			}
		}
	}

	@Scheduled(fixedDelay = 20, timeUnit = TimeUnit.SECONDS, initialDelay = 60)
	public void adapters() {
		propagateAuthenticationContext("adapter-info");
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
		builder.addHeader("expanded", "all");

		List<ClusterMember> members = getClusterMembers();
		if(members.isEmpty()) {
			convertAndSend(builder, "/event/adapters", null);
		} else {
			for (ClusterMember clusterMember : members) {
				UUID id = clusterMember.getId();
				convertAndSend(builder, "/event/"+id.toString()+"/adapters", id);
			}
		}
	}

	private void convertAndSend(RequestMessageBuilder builder, String destination, @Nullable UUID uuid) {
		String jsonResponse = compareAndUpdateResponse(builder, uuid);

		if (jsonResponse != null) {
			this.messagingTemplate.convertAndSend(destination, jsonResponse);
		}
	}
}
