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

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.env.Environment;
import org.springframework.integration.config.IntegrationEvaluationContextFactoryBean;
import org.springframework.integration.config.IntegrationSimpleEvaluationContextFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.DefaultMessageBuilderFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;

import org.frankframework.management.bus.OutboundGateway;
import org.frankframework.management.bus.OutboundGatewayFactory;
import org.frankframework.management.gateway.HttpOutboundGateway;
import org.frankframework.management.security.JwtKeyGenerator;
import org.frankframework.mcp.ManagementGatewayMcpServerFactory;
import org.frankframework.mcp.McpSession;
import org.frankframework.mcp.McpToolProvider;

/**
 * Wires the MCP server that exposes the Frank!Framework Management Gateway. It configures an {@link OutboundGateway} in
 * the same way the Frank!Console does (defaulting to the HTTP gateway so a remote Frank!Framework can be reached), and
 * registers all tool providers.
 */
@Configuration
@ComponentScan("org.frankframework.mcp.tools")
@PropertySource("classpath:application.properties")
public class ManagementGatewayMcpConfiguration {

	private static final String GATEWAY_CLASS_KEY = "management.gateway.outbound.class";

	@Bean
	static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
		return new PropertySourcesPlaceholderConfigurer();
	}

	// Beans required by the (HTTP) outbound gateway and its underlying Spring Integration message handler,
	// mirroring FrankConsoleContext.xml.
	@Bean
	JwtKeyGenerator jwtKeyGenerator() {
		return new JwtKeyGenerator();
	}

	@Bean
	DefaultMessageBuilderFactory messageBuilderFactory() {
		return new DefaultMessageBuilderFactory();
	}

	@Bean
	DefaultConversionService integrationConversionService() {
		return new DefaultConversionService();
	}

	// The Spring Integration message handler underlying the HTTP gateway resolves its SpEL evaluation context from a
	// bean with this exact name during initialization. Registering just this bean (instead of @EnableIntegration) keeps
	// the context free of the extra message channels that would make the handler's autowire-by-type ambiguous.
	@Bean(name = IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)
	IntegrationEvaluationContextFactoryBean integrationEvaluationContext() {
		return new IntegrationEvaluationContextFactoryBean();
	}

	@Bean(name = IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME)
	IntegrationSimpleEvaluationContextFactoryBean integrationSimpleEvaluationContext() {
		return new IntegrationSimpleEvaluationContextFactoryBean();
	}

	@Bean(name = "outboundGateway")
	OutboundGatewayFactory outboundGateway(Environment environment) {
		OutboundGatewayFactory factory = new OutboundGatewayFactory();
		factory.setGatewayClassname(environment.getProperty(GATEWAY_CLASS_KEY, HttpOutboundGateway.class.getCanonicalName()));
		return factory;
	}

	@Bean
	McpSession mcpSession(OutboundGateway outboundGateway) {
		return new McpSession(outboundGateway);
	}

	@Bean
	ObjectMapper mcpObjectMapper() {
		return new ObjectMapper();
	}

	@Bean
	McpJsonMapper mcpJsonMapper(ObjectMapper mcpObjectMapper) {
		return new JacksonMcpJsonMapper(mcpObjectMapper);
	}

	@Bean
	ManagementGatewayMcpServerFactory managementGatewayMcpServerFactory(List<McpToolProvider> toolProviders, McpJsonMapper mcpJsonMapper) {
		return new ManagementGatewayMcpServerFactory(toolProviders, mcpJsonMapper);
	}
}
