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
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import org.frankframework.mcp.AbstractToolProviderTest;

class MessageBrowserToolProviderTest extends AbstractToolProviderTest {

	private List<SyncToolSpecification> tools() {
		return new MessageBrowserToolProvider(sender).getTools();
	}

	@Test
	void browseMessagesSendsFindWithDefaultErrorState() {
		SyncToolSpecification tool = findTool(tools(), "browse_messages");

		call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter", "receiver", "myReceiver"));

		assertThat(transportHeader("topic"), is("MESSAGE_BROWSER"));
		assertThat(transportHeader("action"), is("FIND"));
		assertThat(metaHeader("configuration"), is("myConfig"));
		assertThat(metaHeader("adapter"), is("myAdapter"));
		assertThat(metaHeader("receiver"), is("myReceiver"));
		assertThat(metaHeader("processState"), is("Error"));
	}

	@Test
	void getMessageSendsGetWithMessageId() {
		SyncToolSpecification tool = findTool(tools(), "get_message");

		call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter", "receiver", "myReceiver", "messageId", "1234"));

		assertThat(transportHeader("topic"), is("MESSAGE_BROWSER"));
		assertThat(transportHeader("action"), is("GET"));
		assertThat(metaHeader("messageId"), is("1234"));
	}

	@Test
	void resendMessageSendsAsyncUpload() {
		SyncToolSpecification tool = findTool(tools(), "resend_message");

		CallToolResult result = call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter", "receiver", "myReceiver", "messageId", "1234"));

		assertFalse(result.isError());
		assertTrue(gateway.wasLastCallAsync());
		assertThat(transportHeader("topic"), is("MESSAGE_BROWSER"));
		assertThat(transportHeader("action"), is("UPLOAD"));
		assertThat(metaHeader("messageId"), is("1234"));
	}

	@Test
	void deleteMessageSendsAsyncDelete() {
		SyncToolSpecification tool = findTool(tools(), "delete_message");

		CallToolResult result = call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter", "receiver", "myReceiver", "messageId", "1234"));

		assertFalse(result.isError());
		assertTrue(gateway.wasLastCallAsync());
		assertThat(transportHeader("topic"), is("MESSAGE_BROWSER"));
		assertThat(transportHeader("action"), is("DELETE"));
	}
}
