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
package org.frankframework.mcp.tools;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGateway.ClusterMember;
import org.frankframework.mcp.AbstractToolProvider;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.ToolSchema;

/**
 * Tools to list the members of a (Hazelcast) cluster and to select the member that subsequent requests are routed to,
 * mirroring the {@code ClusterMembers} controller of the Frank!Console. When the configured gateway does not support
 * clustering these tools simply report that there are no members.
 */
public class ClusterMemberToolProvider extends AbstractToolProvider {

	private final ObjectMapper objectMapper;

	public ClusterMemberToolProvider(OutboundGateway outboundGateway, McpSession session, ObjectMapper objectMapper) {
		super(outboundGateway, session);
		this.objectMapper = objectMapper;
	}

	@Override
	public List<SyncToolSpecification> getTools() {
		return List.of(
				listClusterMembers(),
				selectClusterMember());
	}

	private SyncToolSpecification listClusterMembers() {
		return tool("list_cluster_members",
				"List the members of the cluster. The currently selected member is the one requests are routed to.",
				ToolSchema.object().build(),
				request -> {
					List<ClusterMember> members = getMembers();
					if (members.isEmpty()) {
						return "The configured gateway does not expose cluster members.";
					}

					UUID selected = session.getMemberTarget();
					List<Map<String, Object>> result = new ArrayList<>();
					for (ClusterMember member : members) {
						result.add(describe(member, selected));
					}
					return objectMapper.writeValueAsString(result);
				});
	}

	private SyncToolSpecification selectClusterMember() {
		return tool("select_cluster_member",
				"Select the cluster member that subsequent requests should be routed to.",
				ToolSchema.object()
						.requiredString("id", "the id of the member, as returned by list_cluster_members")
						.build(),
				request -> {
					UUID id = UUID.fromString(requiredStringArg(request, "id"));
					session.setMemberTarget(id);
					return "Selected cluster member [%s].".formatted(id);
				});
	}

	private Map<String, Object> describe(ClusterMember member, UUID selected) {
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("id", member.getId() != null ? member.getId().toString() : null);
		result.put("name", member.getName());
		result.put("address", member.getAddress());
		result.put("type", member.getType());
		result.put("localMember", member.isLocalMember());
		result.put("selected", member.getId() != null && member.getId().equals(selected));
		result.put("attributes", member.getAttributes());
		return result;
	}
}
