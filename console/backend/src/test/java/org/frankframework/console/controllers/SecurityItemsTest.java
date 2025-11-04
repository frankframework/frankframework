package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@ContextConfiguration(classes = {WebTestConfiguration.class, SecurityItems.class})
class SecurityItemsTest extends FrankApiTestBase {
	@Test
	public void getSecurityItemsBasic() throws Exception {
		// Empty values without FF except for securityRoles
		mockMvc.perform(MockMvcRequestBuilders.get("/securityitems"))
				.andExpect(MockMvcResultMatchers.status().is(503))
				.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
				.andExpect(MockMvcResultMatchers.jsonPath("authEntries").isEmpty())
				.andExpect(MockMvcResultMatchers.jsonPath("securityRoles").isArray());
	}
}
