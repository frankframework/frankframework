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

import org.springframework.stereotype.Component;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.RequestMessageBuilder;
import org.frankframework.mcp.AbstractToolProvider;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.ToolSchema;

/**
 * Tools to inspect the loaded configurations, mirroring the {@code Configurations} controller of the Frank!Console.
 */
@Component
public class ConfigurationToolProvider extends AbstractToolProvider {

	public ConfigurationToolProvider(OutboundGateway outboundGateway, McpSession session) {
		super(outboundGateway, session);
	}

	@Override
	public List<SyncToolSpecification> getTools() {
		return List.of(
				listConfigurations(),
				getConfiguration(),
				getConfigurationHealth());
	}

	private SyncToolSpecification listConfigurations() {
		return tool("list_configurations",
				"List all configurations, including their version and loaded state.",
				ToolSchema.object().build(),
				request -> sendSyncForString(RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.FIND)));
	}

	private SyncToolSpecification getConfiguration() {
		return tool("get_configuration",
				"Get the (original or loaded) XML of a single configuration.",
				ToolSchema.object()
						.requiredString("configuration", "the name of the configuration")
						.bool("loaded", "when true, return the loaded (property-resolved) configuration instead of the original")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.GET);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, requiredStringArg(request, "configuration"));
					builder.addHeader("loaded", booleanArg(request, "loaded"));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification getConfigurationHealth() {
		return tool("get_configuration_health",
				"Get the health of a single configuration.",
				ToolSchema.object()
						.requiredString("configuration", "the name of the configuration")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.HEALTH);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, requiredStringArg(request, "configuration"));
					return sendSyncForString(builder);
				});
	}
}
