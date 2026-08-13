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

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpServerTransportProvider;

/**
 * Assembles an {@link McpSyncServer} that exposes the Frank!Framework Management Gateway. It collects the tools from all
 * registered {@link McpToolProvider}s and wires them onto the given transport.
 */
public class ManagementGatewayMcpServerFactory {

	private static final String SERVER_NAME = "frank-framework-management-gateway";

	private static final String INSTRUCTIONS = """
			This server exposes the Frank!Framework Management Gateway, use it to inspect and operate a running Frank!Framework instance during development:
			list and inspect adapters and their statistics, start and stop adapters and receivers,
			read log files to find errors, run a test message through an adapter, and browse or manage
			messages in the error store. In a clustered setup, use list_cluster_members and
			select_cluster_member to choose which member requests are routed to.""";

	private final List<McpToolProvider> toolProviders;
	private final McpJsonMapper jsonMapper;

	public ManagementGatewayMcpServerFactory(List<McpToolProvider> toolProviders, McpJsonMapper jsonMapper) {
		this.toolProviders = toolProviders;
		this.jsonMapper = jsonMapper;
	}

	public McpSyncServer create(McpServerTransportProvider transportProvider) {
		return McpServer.sync(transportProvider)
				.serverInfo(SERVER_NAME, determineVersion())
				.capabilities(ServerCapabilities.builder().tools(true).build())
				.instructions(INSTRUCTIONS)
				.tools(getTools())
				.jsonMapper(jsonMapper)
				.build();
	}

	/** All tools that will be exposed, collected from the registered providers. */
	public List<SyncToolSpecification> getTools() {
		return toolProviders.stream()
				.flatMap(provider -> provider.getTools().stream())
				.toList();
	}

	private String determineVersion() {
		String version = getClass().getPackage().getImplementationVersion();
		return version != null ? version : "development";
	}
}
