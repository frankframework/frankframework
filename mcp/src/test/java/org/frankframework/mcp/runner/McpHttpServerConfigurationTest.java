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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import io.modelcontextprotocol.server.McpSyncServer;

import org.frankframework.mcp.StubOutboundGateway;
import org.frankframework.mcp.config.ManagementGatewayMcpConfiguration;

/**
 * Boots the HTTP transport the same way {@link McpServerApplication} does (but on a random port and against a stub
 * gateway) to verify that the embedded Tomcat is created and managed by Spring Boot: there is a {@code tomcat}
 * {@link TomcatServletWebServerFactory} bean, the web server is started by the context and the MCP streamable-HTTP
 * servlet it hosts actually serves requests.
 */
class McpHttpServerConfigurationTest {

	private ConfigurableApplicationContext bootHttpServer() {
		SpringApplication application = new SpringApplication(ManagementGatewayMcpConfiguration.class, McpHttpServerConfiguration.class);
		application.setWebApplicationType(WebApplicationType.SERVLET);
		application.setBannerMode(Banner.Mode.OFF);
		// A random free port keeps the test independent of the environment; the stub gateway avoids needing a running Frank.
		return application.run(
				"--mcp.http.port=0",
				"--management.gateway.outbound.class=" + StubOutboundGateway.class.getCanonicalName());
	}

	@Test
	void springBootStartsAndManagesTheEmbeddedTomcat() throws Exception {
		try (ConfigurableApplicationContext context = bootHttpServer()) {
			// The embedded server is owned by the Spring context (no manual start/stop in the runner).
			assertThat(context, instanceOf(WebServerApplicationContext.class));
			assertThat(context.getBean("tomcat", TomcatServletWebServerFactory.class), notNullValue());

			WebServer webServer = Objects.requireNonNull(((WebServerApplicationContext) context).getWebServer());
			int port = webServer.getPort();
			assertThat("Spring Boot should have started Tomcat on a real port", port, greaterThan(0));

			// All tools are wired onto the server that backs the servlet.
			McpSyncServer server = context.getBean(McpSyncServer.class);
			assertThat(server.listTools(), hasSize(29));

			// The MCP streamable-HTTP servlet is actually reachable through the Spring-managed container: an initialize
			// handshake establishes a session and returns the server info.
			HttpURLConnection connection = (HttpURLConnection) URI.create("http://localhost:" + port + "/mcp").toURL().openConnection();
			try {
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(5000);
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setRequestProperty("Accept", "application/json, text/event-stream");
				connection.setDoOutput(true);
				String initializeRequest = """
						{"jsonrpc":"2.0","id":1,"method":"initialize","params":{\
						"protocolVersion":"2024-11-05","capabilities":{},\
						"clientInfo":{"name":"integration-test","version":"1.0.0"}}}""";
				connection.getOutputStream().write(initializeRequest.getBytes(StandardCharsets.UTF_8));

				assertEquals(HttpURLConnection.HTTP_OK, connection.getResponseCode());
				assertThat(connection.getContentType(), containsString("application/json"));
				assertThat("the streamable transport should establish a session", connection.getHeaderField("mcp-session-id"), notNullValue());

				String body = new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
				assertThat(body, containsString("frank-framework-management-gateway"));
			} finally {
				connection.disconnect();
			}
		}
	}
}
