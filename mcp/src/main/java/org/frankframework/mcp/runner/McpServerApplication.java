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

import java.io.File;
import java.util.concurrent.CountDownLatch;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletSseServerTransportProvider;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

import org.frankframework.mcp.ManagementGatewayMcpServerFactory;
import org.frankframework.mcp.config.ManagementGatewayMcpConfiguration;

/**
 * Stand-alone entry point that starts the Frank!Framework Management Gateway MCP server.
 * <p>
 * The transport is selected with the {@code mcp.transport} property:
 * <ul>
 *   <li>{@code stdio} (default): communicate over standard input/output, the way most MCP clients launch a server.</li>
 *   <li>{@code http}: expose an HTTP (SSE) endpoint on {@code mcp.http.port} (default 3000).</li>
 * </ul>
 * The Management Gateway it talks to is configured through the same properties the Frank!Console uses, most importantly
 * {@code management.gateway.http.outbound.endpoint}.
 */
public class McpServerApplication {

	private static final Logger LOG = LogManager.getLogger(McpServerApplication.class);

	private McpServerApplication() {
		// entry point only
	}

	public static void main(String[] args) throws Exception {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ManagementGatewayMcpConfiguration.class)) {
			Environment environment = context.getEnvironment();
			ManagementGatewayMcpServerFactory serverFactory = context.getBean(ManagementGatewayMcpServerFactory.class);
			McpJsonMapper jsonMapper = context.getBean(McpJsonMapper.class);

			String transport = environment.getProperty("mcp.transport", "stdio");
			LOG.info("starting Frank!Framework Management Gateway MCP server using [{}] transport", transport);

			CountDownLatch shutdownLatch = new CountDownLatch(1);
			if ("http".equalsIgnoreCase(transport)) {
				runHttp(environment, serverFactory, jsonMapper, shutdownLatch);
			} else {
				runStdio(serverFactory, jsonMapper, shutdownLatch);
			}

			shutdownLatch.await();
		}
	}

	private static void runStdio(ManagementGatewayMcpServerFactory serverFactory, McpJsonMapper jsonMapper, CountDownLatch shutdownLatch) {
		StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);
		McpSyncServer server = serverFactory.create(transportProvider);
		LOG.info("MCP server ready with [{}] tools, listening on stdio", server.listTools().size());

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.closeGracefully();
			shutdownLatch.countDown();
		}));
	}

	private static void runHttp(Environment environment, ManagementGatewayMcpServerFactory serverFactory,
			McpJsonMapper jsonMapper, CountDownLatch shutdownLatch) throws Exception {
		int port = environment.getProperty("mcp.http.port", Integer.class, 3000);
		String messageEndpoint = environment.getProperty("mcp.http.messageEndpoint", "/mcp/message");
		String sseEndpoint = environment.getProperty("mcp.http.sseEndpoint", "/sse");

		HttpServletSseServerTransportProvider transportProvider = HttpServletSseServerTransportProvider.builder()
				.jsonMapper(jsonMapper)
				.messageEndpoint(messageEndpoint)
				.sseEndpoint(sseEndpoint)
				.build();

		McpSyncServer server = serverFactory.create(transportProvider);

		Tomcat tomcat = new Tomcat();
		tomcat.setBaseDir(System.getProperty("java.io.tmpdir"));
		tomcat.setPort(port);
		tomcat.getConnector();

		Context tomcatContext = tomcat.addContext("", new File(System.getProperty("java.io.tmpdir")).getAbsolutePath());
		Tomcat.addServlet(tomcatContext, "mcp", transportProvider);
		tomcatContext.addServletMappingDecoded("/*", "mcp");
		tomcat.start();

		LOG.info("MCP server ready with [{}] tools, listening on http://localhost:{}{} (SSE)", server.listTools().size(), port, sseEndpoint);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			server.closeGracefully();
			try {
				tomcat.stop();
				tomcat.destroy();
			} catch (Exception e) {
				LOG.warn("error while stopping the embedded server", e);
			}
			shutdownLatch.countDown();
		}));
	}
}
