package nl.nn.adapterframework.management.bus;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;

import nl.nn.adapterframework.testutil.TestScopeProvider;

public class TestClassInfo extends BusTestBase {

	@Test
	public void getClassByName() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.GET);
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
		MessageBuilder request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.GET);
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
