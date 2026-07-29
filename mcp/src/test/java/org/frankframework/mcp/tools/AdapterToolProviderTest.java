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

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;

import org.frankframework.mcp.AbstractToolProviderTest;

class AdapterToolProviderTest extends AbstractToolProviderTest {

	private List<SyncToolSpecification> tools() {
		return new AdapterToolProvider(sender).getTools();
	}

	@Test
	void listAdaptersSendsAdapterGet() {
		// Arrange
		gateway.setResponsePayload("[]");
		SyncToolSpecification tool = findTool(tools(), "list_adapters");

		// Act
		CallToolResult result = call(tool, Map.of("expanded", true, "showPendingMsgCount", false));

		// Assert
		assertFalse(result.isError());
		assertThat(textOf(result), is("[]"));
		assertThat(transportHeader("topic"), is("ADAPTER"));
		assertThat(transportHeader("action"), is("GET"));
		assertThat(rawMetaHeader("expanded"), is(true));
		assertThat(rawMetaHeader("showPendingMsgCount"), is(false));
	}

	@Test
	void getAdapterSendsAdapterFindWithNames() {
		// Arrange
		SyncToolSpecification tool = findTool(tools(), "get_adapter");

		// Act
		CallToolResult result = call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter"));

		// Assert
		assertFalse(result.isError());
		assertThat(transportHeader("topic"), is("ADAPTER"));
		assertThat(transportHeader("action"), is("FIND"));
		assertThat(metaHeader("configuration"), is("myConfig"));
		assertThat(metaHeader("adapter"), is("myAdapter"));
	}

	@Test
	void getAdapterStatisticsSendsAdapterStatus() {
		SyncToolSpecification tool = findTool(tools(), "get_adapter_statistics");

		call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter"));

		assertThat(transportHeader("topic"), is("ADAPTER"));
		assertThat(transportHeader("action"), is("STATUS"));
	}

	@Test
	void startAdapterSendsAsyncIbisActionWithStartHeader() {
		// Arrange
		SyncToolSpecification tool = findTool(tools(), "start_adapter");

		// Act
		CallToolResult result = call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter"));

		// Assert
		assertFalse(result.isError());
		assertTrue(gateway.wasLastCallAsync());
		assertThat(transportHeader("topic"), is("IBISACTION"));
		assertThat(metaHeader("action"), is("STARTADAPTER"));
		assertThat(metaHeader("configuration"), is("myConfig"));
		assertThat(metaHeader("adapter"), is("myAdapter"));
		assertThat(textOf(result), containsString("STARTADAPTER"));
	}

	@Test
	void stopReceiverSendsAsyncIbisActionWithReceiver() {
		SyncToolSpecification tool = findTool(tools(), "stop_receiver");

		CallToolResult result = call(tool, Map.of("configuration", "myConfig", "adapter", "myAdapter", "receiver", "myReceiver"));

		assertFalse(result.isError());
		assertTrue(gateway.wasLastCallAsync());
		assertThat(transportHeader("topic"), is("IBISACTION"));
		assertThat(metaHeader("action"), is("STOPRECEIVER"));
		assertThat(metaHeader("receiver"), is("myReceiver"));
	}

	@Test
	void missingRequiredArgumentReturnsError() {
		SyncToolSpecification tool = findTool(tools(), "get_adapter");

		CallToolResult result = call(tool, Map.of("configuration", "myConfig"));

		assertTrue(result.isError());
		assertThat(textOf(result), containsString("adapter"));
	}
}
