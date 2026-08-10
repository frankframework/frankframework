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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;

import org.frankframework.mcp.ManagementGatewayMcpServerFactory;

/**
 * Wires the optional streamable-HTTP (SSE) transport as Spring-managed beans, so that Spring Boot owns the lifecycle of
 * the embedded servlet container instead of the runner starting and stopping Tomcat by hand. It is only registered when
 * {@code mcp.transport=http} (see {@link McpServerApplication}).
 * <p>
 * The {@code tomcat} bean is a {@link TomcatServletWebServerFactory}, so it can be retrieved through Spring, and the MCP
 * SSE servlet is registered on it with a {@link ServletRegistrationBean}. Spring Boot starts the container after the
 * context is refreshed and stops it (and closes the MCP server) on shutdown.
 */
@Configuration(proxyBeanMethods = false)
public class McpHttpServerConfiguration {

	@Bean
	TomcatServletWebServerFactory tomcat(@Value("${mcp.http.port:3000}") int port) {
		return new TomcatServletWebServerFactory(port);
	}

	@Bean
	HttpServletSseServerTransportProvider mcpTransportProvider(McpJsonMapper jsonMapper,
			@Value("${mcp.http.messageEndpoint:/mcp/message}") String messageEndpoint,
			@Value("${mcp.http.sseEndpoint:/sse}") String sseEndpoint) {
		return HttpServletSseServerTransportProvider.builder()
				.jsonMapper(jsonMapper)
				.messageEndpoint(messageEndpoint)
				.sseEndpoint(sseEndpoint)
				.build();
	}

	@Bean
	ServletRegistrationBean<HttpServletSseServerTransportProvider> mcpServletRegistration(HttpServletSseServerTransportProvider transportProvider) {
		// The transport provider matches on the full request URI, so map it broadly and let it route the SSE and message endpoints.
		ServletRegistrationBean<HttpServletSseServerTransportProvider> registration = new ServletRegistrationBean<>(transportProvider, "/*");
		registration.setName("mcp");
		registration.setLoadOnStartup(1);
		return registration;
	}

	@Bean(destroyMethod = "closeGracefully")
	McpSyncServer mcpSyncServer(ManagementGatewayMcpServerFactory serverFactory, HttpServletSseServerTransportProvider transportProvider) {
		return serverFactory.create(transportProvider);
	}
}
