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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;

import org.frankframework.mcp.tools.AdapterToolProvider;
import org.frankframework.mcp.tools.ClusterMemberToolProvider;
import org.frankframework.mcp.tools.ConfigurationToolProvider;
import org.frankframework.mcp.tools.LoggingToolProvider;
import org.frankframework.mcp.tools.MessageBrowserToolProvider;
import org.frankframework.mcp.tools.ServerInfoToolProvider;
import org.frankframework.mcp.tools.TestPipelineToolProvider;

class ManagementGatewayMcpServerFactoryTest {

	private ManagementGatewayMcpServerFactory factory;

	@BeforeEach
	void setUp() {
		StubOutboundGateway gateway = new StubOutboundGateway();
		McpSession session = new McpSession(gateway);
		ObjectMapper objectMapper = new ObjectMapper();

		List<McpToolProvider> providers = List.of(
				new AdapterToolProvider(gateway, session),
				new LoggingToolProvider(gateway, session),
				new TestPipelineToolProvider(gateway, session),
				new MessageBrowserToolProvider(gateway, session),
				new ConfigurationToolProvider(gateway, session),
				new ServerInfoToolProvider(gateway, session),
				new ClusterMemberToolProvider(gateway, session, objectMapper));

		factory = new ManagementGatewayMcpServerFactory(providers, new JacksonMcpJsonMapper(objectMapper));
	}

	@Test
	void exposesAllToolsFromEveryProvider() {
		List<SyncToolSpecification> tools = factory.getTools();

		assertThat(tools, hasSize(29));
	}

	@Test
	void everyToolHasAUniqueNonBlankNameAndSchema() {
		List<SyncToolSpecification> tools = factory.getTools();

		List<String> names = tools.stream().map(tool -> tool.tool().name()).toList();
		for (SyncToolSpecification tool : tools) {
			assertFalse(StringUtils.isBlank(tool.tool().name()), "tool name should not be blank");
			assertFalse(StringUtils.isBlank(tool.tool().description()), "tool [" + tool.tool().name() + "] should have a description");
		}
		assertThat("tool names should be unique", names.stream().distinct().count(), is((long) names.size()));
	}
}
