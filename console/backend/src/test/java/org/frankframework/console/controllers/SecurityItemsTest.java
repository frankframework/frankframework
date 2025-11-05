package org.frankframework.console.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {WebTestConfiguration.class, SecurityItems.class})
class SecurityItemsTest extends FrankApiTestBase {
	@Test
	public void getSecurityItemsBasic() throws Exception {
		testActionAndTopicHeaders("/securityitems", "SECURITY_ITEMS", "GET");
	}
}
