package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;

public class TestEnvironmentVariables extends BusTestBase {

	@Test
	public void testEnvironmentVariables() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.ENVIRONMENT);
		Message<String> jsonResponse = (Message<String>) callSyncGateway(request);

		assertThat(jsonResponse.getPayload(), Matchers.containsString("\"test.property\":\"one2drie\""));
	}
}
