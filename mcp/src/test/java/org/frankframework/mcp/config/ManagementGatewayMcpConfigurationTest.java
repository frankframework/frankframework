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
package org.frankframework.mcp.config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;

import org.frankframework.mcp.ManagementGatewayMcpServerFactory;
import org.frankframework.mcp.StubOutboundGateway;

class ManagementGatewayMcpConfigurationTest {

	/**
	 * Boots the full Spring configuration but replaces the outbound gateway with a {@link StubOutboundGateway}, so the
	 * whole bean graph (session, tool providers, server factory) is validated without a running Frank!Framework.
	 */
	@Test
	void springContextWiresAllToolsThroughTheServerFactory() {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
					Map.of("management.gateway.outbound.class", StubOutboundGateway.class.getCanonicalName())));
			context.register(ManagementGatewayMcpConfiguration.class);
			context.refresh();

			ManagementGatewayMcpServerFactory factory = context.getBean(ManagementGatewayMcpServerFactory.class);

			assertThat(factory.getTools(), hasSize(29));
		}
	}
}
