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
import org.springframework.stereotype.Component;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.message.RequestMessageBuilder;
import org.frankframework.mcp.AbstractToolProvider;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.ToolSchema;

/**
 * Tools to browse and manage messages in a transactional storage (such as an error store) of a receiver, mirroring the
 * {@code TransactionalStorage} controller of the Frank!Console.
 */
@Component
public class MessageBrowserToolProvider extends AbstractToolProvider {

	private static final String DEFAULT_PROCESS_STATE = "Error";
	public static final String MESSAGE_ID = "messageId";

	public MessageBrowserToolProvider(OutboundGateway outboundGateway, McpSession session) {
		super(outboundGateway, session);
	}

	@Override
	public List<SyncToolSpecification> getTools() {
		return List.of(
				browseMessages(),
				getMessage(),
				resendMessage(),
				deleteMessage());
	}

	private SyncToolSpecification browseMessages() {
		return tool("browse_messages",
				"Browse the messages in a receiver's message store (the error store by default), with optional filters.",
				ToolSchema.object()
						.requiredString("configuration", "the name of the configuration the adapter belongs to")
						.requiredString("adapter", "the name of the adapter")
						.requiredString("receiver", "the name of the receiver")
						.string("processState", "the message store to browse, e.g. Error (default) or Done")
						.integer("skip", "number of messages to skip")
						.integer("max", "maximum number of messages to return")
						.string(MESSAGE_ID, "filter on message id")
						.string("correlationId", "filter on correlation id")
						.string("type", "filter on message type")
						.string("host", "filter on host")
						.string("message", "filter on (part of) the message content")
						.string("label", "filter on label")
						.string("startDate", "filter on messages inserted after this date")
						.string("endDate", "filter on messages inserted before this date")
						.string("sort", "sort order")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.FIND);
					addStoreHeaders(request, builder);
					builder.addHeader("skip", integerArg(request, "skip"));
					builder.addHeader("max", integerArg(request, "max"));
					builder.addHeader(MESSAGE_ID, stringArg(request, MESSAGE_ID));
					builder.addHeader("correlationId", stringArg(request, "correlationId"));
					builder.addHeader("type", stringArg(request, "type"));
					builder.addHeader("host", stringArg(request, "host"));
					builder.addHeader("message", stringArg(request, "message"));
					builder.addHeader("label", stringArg(request, "label"));
					builder.addHeader("startDate", stringArg(request, "startDate"));
					builder.addHeader("endDate", stringArg(request, "endDate"));
					builder.addHeader("sort", stringArg(request, "sort"));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification getMessage() {
		return tool("get_message",
				"Get the content and metadata of a single message from a receiver's message store.",
				ToolSchema.object()
						.requiredString("configuration", "the name of the configuration the adapter belongs to")
						.requiredString("adapter", "the name of the adapter")
						.requiredString("receiver", "the name of the receiver")
						.string("processState", "the message store, e.g. Error (default) or Done")
						.requiredString(MESSAGE_ID, "the id of the message to retrieve")
						.build(),
				request -> {
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.GET);
					addStoreHeaders(request, builder);
					builder.addHeader(MESSAGE_ID, requiredStringArg(request, MESSAGE_ID));
					return sendSyncForString(builder);
				});
	}

	private SyncToolSpecification resendMessage() {
		return tool("resend_message",
				"Resend (retry) a single message from a receiver's error store.",
				ToolSchema.object()
						.requiredString("configuration", "the name of the configuration the adapter belongs to")
						.requiredString("adapter", "the name of the adapter")
						.requiredString("receiver", "the name of the receiver")
						.requiredString(MESSAGE_ID, "the id of the message to resend")
						.build(),
				request -> {
					String messageId = requiredStringArg(request, MESSAGE_ID);
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.UPLOAD);
					addReceiverHeaders(request, builder);
					builder.addHeader(MESSAGE_ID, messageId);
					sendAsync(builder);
					return "Requested resend of message [%s].".formatted(messageId);
				});
	}

	private SyncToolSpecification deleteMessage() {
		return tool("delete_message",
				"Delete a single message from a receiver's error store.",
				ToolSchema.object()
						.requiredString("configuration", "the name of the configuration the adapter belongs to")
						.requiredString("adapter", "the name of the adapter")
						.requiredString("receiver", "the name of the receiver")
						.requiredString(MESSAGE_ID, "the id of the message to delete")
						.build(),
				request -> {
					String messageId = requiredStringArg(request, MESSAGE_ID);
					RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.MESSAGE_BROWSER, BusAction.DELETE);
					addReceiverHeaders(request, builder);
					builder.addHeader(MESSAGE_ID, messageId);
					sendAsync(builder);
					return "Requested deletion of message [%s].".formatted(messageId);
				});
	}

	private void addReceiverHeaders(CallToolRequest request, RequestMessageBuilder builder) {
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, requiredStringArg(request, "configuration"));
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, requiredStringArg(request, "adapter"));
		builder.addHeader(BusMessageUtils.HEADER_RECEIVER_NAME_KEY, requiredStringArg(request, "receiver"));
	}

	private void addStoreHeaders(CallToolRequest request, RequestMessageBuilder builder) {
		addReceiverHeaders(request, builder);
		String processState = stringArg(request, "processState");
		builder.addHeader("processState", StringUtils.isNotBlank(processState) ? processState : DEFAULT_PROCESS_STATE);
	}
}
