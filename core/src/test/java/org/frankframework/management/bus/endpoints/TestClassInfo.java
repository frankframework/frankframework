package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestScopeProvider;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestClassInfo extends BusTestBase {

	@Test
	public void getClassByName() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.GET);
		String className = TestScopeProvider.class.getCanonicalName();
		request.setHeader("className", className);
		String jsonResponse = (String) callSyncGateway(request).getPayload();

		assertThat(jsonResponse, Matchers.containsString("classLoader\":"));
		assertThat(jsonResponse, Matchers.containsString("specification\":"));
		assertThat(jsonResponse, Matchers.containsString("implementation\":"));
		assertThat(jsonResponse, Matchers.containsString(className.replace(".", "/")));
	}

	@Test
	public void getClassWithBase() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.GET);
		String className = TestScopeProvider.class.getCanonicalName();
		request.setHeader("className", className);
		request.setHeader("baseClassName", this.getClass().getCanonicalName());
		String jsonResponse = (String) callSyncGateway(request).getPayload();

		assertThat(jsonResponse, Matchers.containsString("classLoader\":"));
		assertThat(jsonResponse, Matchers.containsString("specification\":"));
		assertThat(jsonResponse, Matchers.containsString("implementation\":"));
		assertThat(jsonResponse, Matchers.containsString(className.replace(".", "/")));
	}
}
