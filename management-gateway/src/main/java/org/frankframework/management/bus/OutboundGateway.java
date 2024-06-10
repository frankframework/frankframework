/*
   Copyright 2023 - 2024 WeAreFrank!

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
package org.frankframework.management.bus;

import java.util.List;
import java.util.UUID;

import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.messaging.Message;

import lombok.Getter;
import lombok.Setter;

public interface OutboundGateway extends IntegrationPattern {

	@Override
	default IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.outbound_gateway;
	}

	default List<ClusterMember> getMembers() {
		return null;
	}

	@Setter
	@Getter
	public static class ClusterMember {
		private UUID id;
		private String address;
		private String name;
		private boolean localMember;
	}

	/**
	 * I in O out.
	 * @param in Message to send
	 * @return Response message
	 */
	public <I, O> Message<O> sendSyncMessage(Message<I> in);

	/**
	 * I in, no reply
	 * @param in Message to send
	 */
	public <I> void sendAsyncMessage(Message<I> in);
}
