package nl.nn.adapterframework.management.bus;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.testutil.TestConfiguration;

public class TestConfigManagement extends BusTestBase {

	@Test
	public void getOriginalConfigurationByName() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		Message<?> response = callSyncGateway(request);
		assertEquals("original", response.getPayload());
	}

	@Test
	public void getOriginalConfigurations() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		Message<?> response = callSyncGateway(request);
		assertEquals("original", response.getPayload());
	}

	@Test
	public void getLoadedConfigurationByName() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("loaded", true);
		Message<?> response = callSyncGateway(request);
		assertEquals("loaded", response.getPayload());
	}

	@Test
	public void getLoadedConfigurations() {
		MessageBuilder request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		request.setHeader("loaded", true);
		Message<?> response = callSyncGateway(request);
		assertEquals("loaded", response.getPayload());
	}
}
