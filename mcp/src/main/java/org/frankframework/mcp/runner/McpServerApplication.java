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

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.SimpleCommandLinePropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.ResourcePropertySource;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;

import org.frankframework.mcp.ManagementGatewayMcpServerFactory;
import org.frankframework.mcp.config.ManagementGatewayMcpConfiguration;

/**
 * Stand-alone entry point that starts the Frank!Framework Management Gateway MCP server.
 * <p>
 * The transport is selected with the {@code mcp.transport} property:
 * <ul>
 *   <li>{@code stdio} (default): communicate over standard input/output, the way most MCP clients launch a server. No
 *   servlet container is started and a plain application context is used.</li>
 *   <li>{@code http}: expose an HTTP (SSE) endpoint on {@code mcp.http.port} (default 3000). The embedded Tomcat that
 *   serves it is created and managed by Spring Boot (see {@link McpHttpServerConfiguration}).</li>
 * </ul>
 * The Management Gateway it talks to is configured through the same properties the Frank!Console uses, most importantly
 * {@code management.gateway.http.outbound.endpoint}.
 */
public class McpServerApplication {

	private static final Logger LOG = LogManager.getLogger(McpServerApplication.class);

	private static final String TRANSPORT_PROPERTY = "mcp.transport";
	private static final String HTTP_TRANSPORT = "http";
	private static final String DEFAULT_TRANSPORT = "stdio";

	private McpServerApplication() {
		// entry point only
	}

	public static void main(String[] args) throws Exception {
		String transport = resolveTransport(args);
		LOG.info("starting Frank!Framework Management Gateway MCP server using [{}] transport", transport);

		if (HTTP_TRANSPORT.equalsIgnoreCase(transport)) {
			runHttp(args);
		} else {
			runStdio();
		}
	}

	/**
	 * Resolve the configured transport before the application context is booted, so the right kind of context can be
	 * created (a plain one for stdio, a Spring Boot servlet web-server context for http). Command-line arguments, system
	 * properties and environment variables take precedence over the packaged {@code application.properties}, matching
	 * the way the configuration is resolved once the context is up.
	 */
	private static String resolveTransport(String[] args) throws IOException {
		StandardEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.addFirst(new SimpleCommandLinePropertySource(args));
		propertySources.addLast(new ResourcePropertySource("application.properties", new ClassPathResource("application.properties")));
		return environment.getProperty(TRANSPORT_PROPERTY, DEFAULT_TRANSPORT);
	}

	/**
	 * Run the stdio transport. Stdout is reserved for the JSON-RPC protocol, so no servlet container is started; the
	 * plain application context is kept open until the JVM is asked to shut down.
	 */
	private static void runStdio() throws InterruptedException {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ManagementGatewayMcpConfiguration.class)) {
			ManagementGatewayMcpServerFactory serverFactory = context.getBean(ManagementGatewayMcpServerFactory.class);
			McpJsonMapper jsonMapper = context.getBean(McpJsonMapper.class);

			StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);
			McpSyncServer server = serverFactory.create(transportProvider);
			LOG.info("MCP server ready with [{}] tools, listening on stdio", server.listTools().size());

			CountDownLatch shutdownLatch = new CountDownLatch(1);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				server.closeGracefully();
				shutdownLatch.countDown();
			}));

			shutdownLatch.await();
		}
	}

	/**
	 * Run the streamable-HTTP (SSE) transport. The embedded Tomcat that serves the MCP servlet is created and managed by
	 * Spring Boot (see {@link McpHttpServerConfiguration}); its non-daemon threads keep the JVM alive and Spring Boot
	 * stops the server and closes the MCP server on shutdown.
	 */
	private static void runHttp(String[] args) {
		SpringApplication application = new SpringApplication(ManagementGatewayMcpConfiguration.class, McpHttpServerConfiguration.class);
		application.setWebApplicationType(WebApplicationType.SERVLET);
		application.setBannerMode(Banner.Mode.OFF);
		ConfigurableApplicationContext context = application.run(args);

		Environment environment = context.getEnvironment();
		McpSyncServer server = context.getBean(McpSyncServer.class);
		int port = environment.getProperty("mcp.http.port", Integer.class, 3000);
		String sseEndpoint = environment.getProperty("mcp.http.sseEndpoint", "/sse");
		LOG.info("MCP server ready with [{}] tools, listening on http://localhost:{}{} (SSE)", server.listTools().size(), port, sseEndpoint);
	}
}
