package org.frankframework.console.configuration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.frankframework.console.controllers.ServerDetails;
import org.frankframework.console.controllers.WebTestConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SecurityChainTestConfiguration.class, SecurityChainConfigurer.class, WebTestConfiguration.class, ServerDetails.class })
@WebAppConfiguration
public class SecurityChainConfigurerWithoutAuthenticationTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("dtap.stage", () -> "STUB");

		registry.add("application.security.console.authentication.type", () -> "NONE");
		registry.add("application.security.http.authenticators", () -> "inMem");
		registry.add("application.security.http.authenticators.inMem.type", () -> "NONE");
		registry.add("servlet.LarvaServlet.authenticator", () -> "inMem");
		registry.add("servlet.LarvaServlet.securityRoles", () -> "IbisAdmin");
		registry.add("servlet.WebContentServlet.authenticator", () -> "inMem");
		registry.add("servlet.WebContentServlet.securityRoles", () -> "IbisAdmin");
	}

	@BeforeEach
	public void beforeEach() {
		mockMvc = MockMvcBuilders
				.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@Test
	void testInfoEndpoint() throws Exception {
		// without authentication
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/info"))
				.andExpect(status().isOk());

		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/info")
						.remoteAddress("195.1.12.1"))
				.andExpect(status().isOk());
	}

	@Test
	void testServerHealthEndpoint() throws Exception {
		// use localhost as remote address, not authorized
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/health"))
				.andExpect(status().isOk());

		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/health")
						.remoteAddress("195.1.12.1"))
				.andExpect(status().isOk());
	}
}
