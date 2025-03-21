package org.frankframework.console.configuration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.frankframework.console.controllers.ServerDetails;
import org.frankframework.console.controllers.WebTestConfiguration;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { SecurityChainTestConfiguration.class, SecurityChainConfigurer.class, WebTestConfiguration.class, ServerDetails.class })
@WebAppConfiguration
public class SecurityChainConfigurerTest {

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@DynamicPropertySource
	static void dynamicProperties(DynamicPropertyRegistry registry) {
		registry.add("dtap.stage", () -> "ACC");
		registry.add("application.security.console.authentication.type", () -> "IN_MEMORY");
		registry.add("application.security.console.authentication.username", () -> "Admin");
		registry.add("application.security.console.authentication.password", () -> "Nimda");
	}

	@BeforeEach
	public void beforeEach() {
		mockMvc = MockMvcBuilders
				.webAppContextSetup(context)
				.apply(springSecurity())
				.build();
	}

	@WithMockUser(authorities = "ROLE_ADMIN")
	@Test
	void testServerHealthWithLoggedInUser() throws Exception {
		// used from localhost
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/health"))
				.andExpect(status().isOk());

		// custom remote address
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/health")
						.remoteAddress("195.1.12.1"))
				.andExpect(status().isOk());
	}

	@Test
	void testInfoEndpoint() throws Exception {
		// without authentication
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/info"))
				.andExpect(status().isUnauthorized());

		// with authentication
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/info")
						.with(httpBasic("Admin", "Nimda")))
				.andDo(MockMvcResultHandlers.print())
				.andExpect(status().isOk());
	}

	@Test
	void testServerHealthEndpoint() throws Exception {
		// use localhost as remote address, not authorized
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/health"))
				.andExpect(status().isOk());

		// custom remote address, not authorized
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/health")
						.remoteAddress("192.168.0.1"))
				.andExpect(status().isUnauthorized());

		// custom remote address, authorized
		this.mockMvc.perform(MockMvcRequestBuilders.get("/server/health")
						.with(httpBasic("Admin", "Nimda"))
						.remoteAddress("192.168.0.1"))
				.andExpect(status().isOk());
	}
}
