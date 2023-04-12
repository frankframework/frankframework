package nl.nn.adapterframework.management.bus.endpoints;

import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestConfiguration;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestMonitoring extends BusTestBase {

	@Test
	public void getMonitors() throws Exception {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.MONITORING, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);

		// Act
		Message<?> response = callSyncGateway(request);

		// Assert
		String expectedJson = TestFileUtils.getTestFile("/Management/getMonitors.json");
		String payload = (String) response.getPayload();
		MatchUtils.assertJsonEquals(expectedJson, payload);
	}
}