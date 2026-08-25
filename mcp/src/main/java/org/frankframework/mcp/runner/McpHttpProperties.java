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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The {@code mcp.http.*} settings for the optional streamable-HTTP transport (only used when
 * {@code mcp.transport=http}). Grouping them in a single {@link ConfigurationProperties} type keeps
 * {@link McpHttpServerConfiguration} readable instead of scattering individual {@code @Value} lookups.
 */
@ConfigurationProperties(prefix = "mcp.http")
public class McpHttpProperties {

	/** The port the embedded servlet container listens on. */
	private int port = 3000;

	/** The single endpoint that serves the streamable-HTTP (MCP) transport. */
	private String mcpEndpoint = "/mcp";

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getMcpEndpoint() {
		return mcpEndpoint;
	}

	public void setMcpEndpoint(String mcpEndpoint) {
		this.mcpEndpoint = mcpEndpoint;
	}
}
