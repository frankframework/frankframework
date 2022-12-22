package nl.nn.adapterframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.TestScopeProvider;

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
