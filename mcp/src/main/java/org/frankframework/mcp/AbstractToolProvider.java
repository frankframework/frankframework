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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.NonNull;
import org.springframework.messaging.Message;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGateway.ClusterMember;
import org.frankframework.management.bus.message.RequestMessageBuilder;

/**
 * Base class for {@link McpToolProvider}s. It talks to the (possibly remote) Frank!Framework Management Gateway, in the
 * same way the Frank!Console's {@code FrankApiService} does, and offers small helpers to declare tools, read their
 * arguments and turn a result (or exception) into an MCP {@link CallToolResult}, so that concrete providers only need to
 * describe the gateway request.
 */
public abstract class AbstractToolProvider implements McpToolProvider {

	protected final Logger log = LogManager.getLogger(this);

	protected final OutboundGateway outboundGateway;
	protected final McpSession session;

	protected AbstractToolProvider(OutboundGateway outboundGateway, McpSession session) {
		this.outboundGateway = outboundGateway;
		this.session = session;
	}

	/**
	 * Send a request and wait for the response.
	 *
	 * @return the response message, never {@literal null}
	 */
	@NonNull
	protected Message<?> sendSync(RequestMessageBuilder builder) {
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
	protected String sendSyncForString(RequestMessageBuilder builder) {
		String payload = BusMessageUtils.getPayloadAsString(sendSync(builder));
		return payload != null ? payload : "";
	}

	/** Send a request without waiting for (or expecting) a response. */
	protected void sendAsync(RequestMessageBuilder builder) {
		outboundGateway.sendAsyncMessage(builder.build(session.getMemberTarget()));
	}

	/** All members that are part of the (Hazelcast) cluster. Empty when the gateway does not support clustering. */
	@NonNull
	protected List<ClusterMember> getMembers() {
		return outboundGateway.getMembers();
	}

	/** The logic behind a tool: it receives the (validated) request and produces the textual result. */
	@FunctionalInterface
	protected interface ToolCall {
		String apply(CallToolRequest request) throws Exception; // NOSONAR allow tools to signal any failure
	}

	/**
	 * Declare a tool.
	 *
	 * @param name        the unique tool name, as exposed to the MCP client
	 * @param description a human/LLM readable description of what the tool does
	 * @param inputSchema a JSON-schema describing the tool arguments (see {@link ToolSchema})
	 * @param call        the logic that is invoked when the tool is called
	 */
	protected SyncToolSpecification tool(String name, String description, Map<String, Object> inputSchema, ToolCall call) {
		Tool tool = Tool.builder(name, inputSchema)
				.description(description)
				.build();

		return SyncToolSpecification.builder()
				.tool(tool)
				.callHandler((exchange, request) -> invoke(name, call, request))
				.build();
	}

	private CallToolResult invoke(String name, ToolCall call, CallToolRequest request) {
		try {
			String result = call.apply(request);
			return CallToolResult.builder()
					.addTextContent(StringUtils.isNotEmpty(result) ? result : "(no content)")
					.isError(false)
					.build();
		} catch (Exception e) { // NOSONAR any failure must be reported back to the MCP client
			log.warn("tool [{}] failed", name, e);
			return CallToolResult.builder()
					.addTextContent("Error: " + e.getMessage())
					.isError(true)
					.build();
		}
	}

	protected static String stringArg(CallToolRequest request, String name) {
		Object value = request.arguments().get(name);
		return value != null ? value.toString() : null;
	}

	protected static String requiredStringArg(CallToolRequest request, String name) {
		String value = stringArg(request, name);
		if (StringUtils.isBlank(value)) {
			throw new IllegalArgumentException("missing required argument [" + name + "]");
		}
		return value;
	}

	protected static Boolean booleanArg(CallToolRequest request, String name) {
		Object value = request.arguments().get(name);
		if (value == null) {
			return null;
		}
		if (value instanceof Boolean bool) {
			return bool;
		}
		return Boolean.valueOf(value.toString());
	}

	protected static Integer integerArg(CallToolRequest request, String name) {
		Object value = request.arguments().get(name);
		if (value == null) {
			return null;
		}
		if (value instanceof Number number) {
			return number.intValue();
		}
		return Integer.valueOf(value.toString());
	}
}
