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
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.Getter;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGateway.ClusterMember;

/**
 * Keeps track of the cluster member (a Frank!Framework worker) that requests are routed to.
 * <p>
 * When the gateway supports clustering (Hazelcast) the framework can consist of multiple members. Just like the
 * Frank!Console keeps a selected member in the HTTP session, the MCP server keeps the selected member for the lifetime
 * of the process so subsequent tool calls target the same worker.
 * <p>
 * For gateways that do not expose members (such as the HTTP gateway) the target is {@literal null}, which lets the
 * gateway route to its single, implicit destination.
 */
public class McpSession {

	private final OutboundGateway outboundGateway;

	private @Nullable @Getter UUID memberTarget;

	public McpSession(OutboundGateway outboundGateway) {
		this.outboundGateway = outboundGateway;
		this.memberTarget = findDefaultWorker().orElse(null);
	}

	/**
	 * Select the member that subsequent requests should be routed to.
	 *
	 * @param id the id of a known {@code worker} member
	 * @throws IllegalArgumentException when no member with the given id exists, or it is not a {@code worker}
	 */
	public void setMemberTarget(UUID id) {
		ClusterMember member = outboundGateway.getMembers().stream()
				.filter(candidate -> id.equals(candidate.getId()))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("member target with id [" + id + "] not found"));

		if (!"worker".equals(member.getType())) {
			throw new IllegalArgumentException("member target with id [" + id + "] is of type [" + member.getType() + "], only [worker] members can be selected");
		}

		this.memberTarget = id;
	}

	private Optional<UUID> findDefaultWorker() {
		List<ClusterMember> members = outboundGateway.getMembers();
		return members.stream()
				.filter(member -> "worker".equals(member.getType()))
				.map(ClusterMember::getId)
				.findFirst();
	}
}
