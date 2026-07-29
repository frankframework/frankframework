/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.mcp;

import java.util.List;

import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;

import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGateway.ClusterMember;
import org.frankframework.management.bus.message.RequestMessageBuilder;

/**
 * Sends messages to the Frank!Framework Management Gateway, in the same way the Frank!Console's {@code FrankApiService}
 * does. It is the single point where MCP tools talk to a (possibly remote) Frank!Framework instance.
 */
public class ManagementGatewaySender {

	private final OutboundGateway outboundGateway;
	private final McpSession session;

	public ManagementGatewaySender(OutboundGateway outboundGateway, McpSession session) {
		this.outboundGateway = outboundGateway;
		this.session = session;
	}

	/**
	 * Send a request and wait for the response.
	 *
	 * @return the response message, never {@literal null}
	 */
	@NonNull
	public Message<?> sendSync(RequestMessageBuilder builder) {
		try {
			return outboundGateway.sendSyncMessage(builder.build(session.getMemberTarget()));
		} catch (BusException e) {
			throw new McpGatewayException("error sending request to the Frank!Framework: " + e.getMessage(), e);
		}
	}

	/**
	 * Send a request, wait for the response and return its payload as a String.
	 *
	 * @return the response payload, or an empty String when the response has no content
	 */
	@NonNull
	public String sendSyncForString(RequestMessageBuilder builder) {
		String payload = BusMessageUtils.getPayloadAsString(sendSync(builder));
		return payload != null ? payload : "";
	}

	/** Send a request without waiting for (or expecting) a response. */
	public void sendAsync(RequestMessageBuilder builder) {
		outboundGateway.sendAsyncMessage(builder.build(session.getMemberTarget()));
	}

	/** All members that are part of the (Hazelcast) cluster. Empty when the gateway does not support clustering. */
	@NonNull
	public List<ClusterMember> getMembers() {
		return outboundGateway.getMembers();
	}

	public McpSession getSession() {
		return session;
	}
}
