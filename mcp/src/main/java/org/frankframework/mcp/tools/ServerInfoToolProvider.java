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

import java.util.List;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.RequestMessageBuilder;
import org.frankframework.mcp.AbstractToolProvider;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.ToolSchema;

/**
 * Tools that expose general server information, mirroring the {@code ServerDetails}, {@code EnvironmentVariables},
 * {@code SecurityItems}, {@code Scheduler} and {@code ConnectionOverview} controllers of the Frank!Console.
 */
public class ServerInfoToolProvider extends AbstractToolProvider {

	public ServerInfoToolProvider(OutboundGateway outboundGateway, McpSession session) {
		super(outboundGateway, session);
	}

	@Override
	public List<SyncToolSpecification> getTools() {
		return List.of(
				getServerInfo(),
				getServerWarnings(),
				getHealth(),
				getEnvironmentVariables(),
				getSecurityItems(),
				getScheduler(),
				getConnectionOverview());
	}

	private SyncToolSpecification getServerInfo() {
		return tool("get_server_info",
				"Get general information about the running Frank!Framework instance (version, uptime, machine name, configurations).",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.GET)));
	}

	private SyncToolSpecification getServerWarnings() {
		return tool("get_server_warnings",
				"Get the configuration warnings and startup errors of the running Frank!Framework instance.",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.WARNINGS)));
	}

	private SyncToolSpecification getHealth() {
		return tool("get_health",
				"Get the overall health of the running Frank!Framework instance.",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.HEALTH)));
	}

	private SyncToolSpecification getEnvironmentVariables() {
		return tool("get_environment_variables",
				"Get the environment variables and system properties of the running Frank!Framework instance.",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.ENVIRONMENT)));
	}

	private SyncToolSpecification getSecurityItems() {
		return tool("get_security_items",
				"Get an overview of security related items (authentication entries, roles, certificates).",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.SECURITY_ITEMS)));
	}

	private SyncToolSpecification getScheduler() {
		return tool("get_scheduler",
				"Get the scheduler and its scheduled jobs.",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.SCHEDULER, BusAction.GET)));
	}

	private SyncToolSpecification getConnectionOverview() {
		return tool("get_connection_overview",
				"Get an overview of all connections (listeners, senders) used by the adapters.",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.CONNECTION_OVERVIEW)));
	}
}
