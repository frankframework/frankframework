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
package org.frankframework.mcp.runner;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;

import org.frankframework.mcp.ManagementGatewayMcpServerFactory;

/**
 * Wires the optional streamable-HTTP transport as Spring-managed beans, so that Spring Boot owns the lifecycle of the
 * embedded servlet container instead of the runner starting and stopping Tomcat by hand. It is only registered when
 * {@code mcp.transport=http} (see {@link McpServerApplication}).
 * <p>
 * The {@code tomcat} bean is a {@link TomcatServletWebServerFactory}, so it can be retrieved through Spring, and the MCP
 * streamable-HTTP servlet is registered on it with a {@link ServletRegistrationBean}. Spring Boot starts the container
 * after the context is refreshed and stops it (and closes the MCP server) on shutdown. The {@code mcp.http.*} settings
 * are bound onto {@link McpHttpProperties}.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(McpHttpProperties.class)
public class McpHttpServerConfiguration {

	@Bean
	TomcatServletWebServerFactory tomcat(McpHttpProperties properties) {
		return new TomcatServletWebServerFactory(properties.getPort());
	}

	@Bean
	HttpServletStreamableServerTransportProvider mcpTransportProvider(McpJsonMapper jsonMapper, McpHttpProperties properties) {
		return HttpServletStreamableServerTransportProvider.builder()
				.jsonMapper(jsonMapper)
				.mcpEndpoint(properties.getMcpEndpoint())
				.build();
	}

	@Bean
	ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServletRegistration(HttpServletStreamableServerTransportProvider transportProvider,
			McpHttpProperties properties) {
		// The transport provider serves the single MCP endpoint (GET for the SSE stream, POST for messages, DELETE to end a session).
		ServletRegistrationBean<HttpServletStreamableServerTransportProvider> registration = new ServletRegistrationBean<>(transportProvider, properties.getMcpEndpoint());
		registration.setName("mcp");
		registration.setLoadOnStartup(1);
		return registration;
	}

	@Bean(destroyMethod = "closeGracefully")
	McpSyncServer mcpSyncServer(ManagementGatewayMcpServerFactory serverFactory, HttpServletStreamableServerTransportProvider transportProvider) {
		return serverFactory.create(transportProvider);
	}
}
