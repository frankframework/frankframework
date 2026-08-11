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

import org.apache.commons.lang3.StringUtils;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.AbstractMessage;
import org.frankframework.management.bus.message.RequestMessageBuilder;
import org.frankframework.mcp.AbstractToolProvider;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.ToolSchema;

/**
 * Tool to test a pipeline (adapter) with a supplied input message, mirroring the {@code TestPipeline} controller of the
 * Frank!Console. Both the resulting state and the result message are returned.
 */
@Component
public class TestPipelineToolProvider extends AbstractToolProvider {

	public TestPipelineToolProvider(OutboundGateway outboundGateway, McpSession session) {
		super(outboundGateway, session);
	}

	@Override
	public List<SyncToolSpecification> getTools() {
		return List.of(testPipeline());
	}

	private SyncToolSpecification testPipeline() {
		return tool("test_pipeline",
				"Run a test message through an adapter and return the resulting state (SUCCESS/ERROR) and the result message.",
				ToolSchema.object()
						.requiredString("configuration", "the name of the configuration the adapter belongs to")
						.requiredString("adapter", "the name of the adapter to test")
						.requiredString("message", "the input message to send through the adapter")
						.string("sessionKeys", "optional session keys as a JSON array, e.g. [{\"key\":\"k\",\"value\":\"v\"}]")
						.build(),
				request -> {
					String message = requiredStringArg(request, "message");

					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.TEST_PIPELINE, BusAction.UPLOAD);
					builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, requiredStringArg(request, "configuration"));
					builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, requiredStringArg(request, "adapter"));

					String sessionKeys = stringArg(request, "sessionKeys");
					if (StringUtils.isNotBlank(sessionKeys)) {
						builder.addHeader("sessionKeys", sessionKeys);
					}
					builder.setPayload(message);

					Message<?> response = sendSync(builder);
					String state = BusMessageUtils.getHeader(response, AbstractMessage.STATE_KEY);
					String result = BusMessageUtils.getPayloadAsString(response);
					return "state: %s%nresult:%n%s".formatted(state, result != null ? result : "");
				});
	}
}
