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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import org.frankframework.management.bus.OutboundGateway.ClusterMember;
import org.frankframework.mcp.AbstractToolProviderTest;
import org.frankframework.mcp.McpSession;

class ClusterMemberToolProviderTest extends AbstractToolProviderTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private List<SyncToolSpecification> tools() {
		return new ClusterMemberToolProvider(gateway, session, OBJECT_MAPPER).getTools();
	}

	private static ClusterMember member(UUID id, String name, String type) {
		ClusterMember member = new ClusterMember();
		member.setId(id);
		member.setName(name);
		member.setType(type);
		return member;
	}

	/** Re-create the session so a freshly configured set of members is picked up. */
	private void refreshSession() {
		session = new McpSession(gateway);
	}

	@Test
	void listReportsNoMembersWhenGatewayHasNone() {
		SyncToolSpecification tool = findTool(tools(), "list_cluster_members");

		CallToolResult result = call(tool, Map.of());

		assertFalse(result.isError());
		assertThat(textOf(result), containsString("does not expose"));
	}

	@Test
	void listReturnsMembersAsJsonWithDefaultWorkerSelected() {
		UUID worker = UUID.randomUUID();
		gateway.setMembers(List.of(member(worker, "node-1", "worker"), member(UUID.randomUUID(), "ctrl", "controller")));
		refreshSession();
		SyncToolSpecification tool = findTool(tools(), "list_cluster_members");

		CallToolResult result = call(tool, Map.of());

		assertFalse(result.isError());
		String text = textOf(result);
		assertThat(text, containsString("node-1"));
		assertThat(text, containsString(worker.toString()));
		assertThat(session.getMemberTarget(), is(worker));
	}

	@Test
	void selectClusterMemberUpdatesTarget() {
		UUID worker = UUID.randomUUID();
		gateway.setMembers(List.of(member(worker, "node-1", "worker")));
		SyncToolSpecification tool = findTool(tools(), "select_cluster_member");

		CallToolResult result = call(tool, Map.of("id", worker.toString()));

		assertFalse(result.isError());
		assertThat(session.getMemberTarget(), is(worker));
	}

	@Test
	void selectUnknownMemberReturnsError() {
		gateway.setMembers(List.of(member(UUID.randomUUID(), "node-1", "worker")));
		SyncToolSpecification tool = findTool(tools(), "select_cluster_member");

		CallToolResult result = call(tool, Map.of("id", UUID.randomUUID().toString()));

		assertTrue(result.isError());
	}
}
