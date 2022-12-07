package nl.nn.adapterframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;

public class TestEnvironmentVariables extends BusTestBase {

	@Test
	public void testEnvironmentVariables() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.ENVIRONMENT);
		Message<String> jsonResponse = (Message<String>) callSyncGateway(request);

		assertThat(jsonResponse.getPayload(), Matchers.containsString("\"test.property\":\"one2drie\""));
	}
}
