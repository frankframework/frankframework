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
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;

import org.frankframework.management.bus.BusMessageUtils;

/**
 * Base class for tool provider tests. It wires a {@link StubOutboundGateway} so the request a tool produces can be
 * inspected, and offers helpers to invoke a tool and read its result.
 */
public abstract class AbstractToolProviderTest {

	protected StubOutboundGateway gateway;
	protected McpSession session;
	protected ManagementGatewaySender sender;

	@BeforeEach
	void setUpGateway() {
		gateway = new StubOutboundGateway();
		session = new McpSession(gateway);
		sender = new ManagementGatewaySender(gateway, session);
	}

	protected static SyncToolSpecification findTool(List<SyncToolSpecification> tools, String name) {
		return tools.stream()
				.filter(tool -> tool.tool().name().equals(name))
				.findFirst()
				.orElseThrow(() -> new AssertionError("tool [" + name + "] not found"));
	}

	protected static CallToolResult call(SyncToolSpecification tool, Map<String, Object> arguments) {
		return tool.callHandler().apply(null, new CallToolRequest(tool.tool().name(), arguments));
	}

	protected static String textOf(CallToolResult result) {
		return result.content().stream()
				.filter(TextContent.class::isInstance)
				.map(content -> ((TextContent) content).text())
				.collect(Collectors.joining());
	}

	/** Read a custom (data-layer) header from the last message the gateway received. */
	protected String metaHeader(String key) {
		return BusMessageUtils.getHeader(gateway.getLastSentMessage(), key);
	}

	/** Read the raw (unconverted) value of a custom header, e.g. to inspect a {@link Boolean} or {@link Integer} header. */
	protected Object rawMetaHeader(String key) {
		return gateway.getLastSentMessage().getHeaders().get(BusMessageUtils.HEADER_PREFIX + key);
	}

	/** Read a transport header (such as {@code topic} or {@code action}) from the last message the gateway received. */
	protected Object transportHeader(String key) {
		return gateway.getLastSentMessage().getHeaders().get(key);
	}
}
