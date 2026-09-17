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
import java.util.Map;

import org.springframework.stereotype.Component;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import org.frankframework.management.Action;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.RequestMessageBuilder;
import org.frankframework.mcp.AbstractToolProvider;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.ToolSchema;

/**
 * Tools to inspect and control adapters, mirroring the {@code Adapters} controller of the Frank!Console.
 */
@Component
public class AdapterToolProvider extends AbstractToolProvider {

	public static final String CONFIGURATION = "configuration";
	public static final String ADAPTER = "adapter";

	public AdapterToolProvider(OutboundGateway outboundGateway, McpSession session) {
		super(outboundGateway, session);
	}

	@Override
	public List<SyncToolSpecification> getTools() {
		return List.of(
				listAdapters(),
				getAdapter(),
				getAdapterStatistics(),
				getAdapterHealth(),
				startAdapter(),
				stopAdapter(),
				startReceiver(),
				stopReceiver());
	}

	private SyncToolSpecification listAdapters() {
		return tool("list_adapters",
				"List all adapters of all configurations, including their running state and message counts.",
				ToolSchema.object()
						.bool("expanded", "when true, include receivers, pipes and messages of each adapter")
						.bool("showPendingMsgCount", "when true, include the number of pending messages")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.GET);
					builder.addHeader("expanded", booleanArg(request, "expanded"));
					builder.addHeader("showPendingMsgCount", booleanArg(request, "showPendingMsgCount"));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification getAdapter() {
		return tool("get_adapter",
				"Get the details (receivers, pipes and running state) of a single adapter.",
				ToolSchema.object()
						.requiredString(CONFIGURATION, "the name of the configuration the adapter belongs to")
						.requiredString(ADAPTER, "the name of the adapter")
						.bool("expanded", "when true, include receivers, pipes and messages")
						.bool("showPendingMsgCount", "when true, include the number of pending messages")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.FIND);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, requiredStringArg(request, CONFIGURATION));
					builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, requiredStringArg(request, ADAPTER));
					builder.addHeader("expanded", booleanArg(request, "expanded"));
					builder.addHeader("showPendingMsgCount", booleanArg(request, "showPendingMsgCount"));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification getAdapterStatistics() {
		return tool("get_adapter_statistics",
				"Get processing statistics (durations, counts, sizes) of a single adapter.",
				ToolSchema.object()
						.requiredString(CONFIGURATION, "the name of the configuration the adapter belongs to")
						.requiredString(ADAPTER, "the name of the adapter")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.ADAPTER, BusAction.STATUS);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, requiredStringArg(request, CONFIGURATION));
					builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, requiredStringArg(request, ADAPTER));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification getAdapterHealth() {
		return tool("get_adapter_health",
				"Get the health of an adapter, or of the whole application when no adapter is given.",
				ToolSchema.object()
						.string(CONFIGURATION, "the name of the configuration the adapter belongs to")
						.string(ADAPTER, "the name of the adapter")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.HEALTH);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, stringArg(request, CONFIGURATION));
					builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, stringArg(request, ADAPTER));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification startAdapter() {
		return tool("start_adapter",
				"Start an adapter.",
				adapterActionSchema(),
				request -> sendAdapterAction(request, Action.STARTADAPTER));
	}

	private SyncToolSpecification stopAdapter() {
		return tool("stop_adapter",
				"Stop an adapter.",
				adapterActionSchema(),
				request -> sendAdapterAction(request, Action.STOPADAPTER));
	}

	private SyncToolSpecification startReceiver() {
		return tool("start_receiver",
				"Start a receiver of an adapter.",
				receiverActionSchema(),
				request -> sendReceiverAction(request, Action.STARTRECEIVER));
	}

	private SyncToolSpecification stopReceiver() {
		return tool("stop_receiver",
				"Stop a receiver of an adapter.",
				receiverActionSchema(),
				request -> sendReceiverAction(request, Action.STOPRECEIVER));
	}

	private String sendAdapterAction(CallToolRequest request, Action action) {
		String configuration = requiredStringArg(request, CONFIGURATION);
		String adapter = requiredStringArg(request, ADAPTER);

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISACTION);
		builder.addHeader("action", action.name());
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		sendAsync(builder);

		return "Requested %s for adapter [%s] in configuration [%s].".formatted(action.name(), adapter, configuration);
	}

	private String sendReceiverAction(CallToolRequest request, Action action) {
		String configuration = requiredStringArg(request, CONFIGURATION);
		String adapter = requiredStringArg(request, ADAPTER);
		String receiver = requiredStringArg(request, "receiver");

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISACTION);
		builder.addHeader("action", action.name());
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, receiver);
		sendAsync(builder);

		return "Requested %s for receiver [%s] of adapter [%s] in configuration [%s]."
				.formatted(action.name(), receiver, adapter, configuration);
	}

	private Map<String, Object> adapterActionSchema() {
		return ToolSchema.object()
				.requiredString(CONFIGURATION, "the name of the configuration the adapter belongs to")
				.requiredString(ADAPTER, "the name of the adapter")
				.build();
	}

	private Map<String, Object> receiverActionSchema() {
		return ToolSchema.object()
				.requiredString(CONFIGURATION, "the name of the configuration the adapter belongs to")
				.requiredString(ADAPTER, "the name of the adapter")
				.requiredString("receiver", "the name of the receiver")
				.build();
	}
}
