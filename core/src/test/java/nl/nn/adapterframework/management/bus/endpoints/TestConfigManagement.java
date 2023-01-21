package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.TestConfiguration;

public class TestConfigManagement extends BusTestBase {
	private static final String LOADED_RESULT = "<loaded authAlias=\"test\" />";
	private static final String ORIGINAL_RESULT = "<original authAlias=\"test\" />";

	@Test
	public void getOriginalConfigurationByName() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		Message<?> response = callSyncGateway(request);
		assertEquals(ORIGINAL_RESULT, response.getPayload());
	}

	@Test
	public void getOriginalConfigurations() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		Message<?> response = callSyncGateway(request);
		assertEquals(ORIGINAL_RESULT, response.getPayload());
	}

	@Test
	public void getLoadedConfigurationByName() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
		request.setHeader("loaded", true);
		Message<?> response = callSyncGateway(request);
		assertEquals(LOADED_RESULT, response.getPayload());
	}

	@Test
	public void getLoadedConfigurations() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.GET);
		request.setHeader("loaded", true);
		Message<?> response = callSyncGateway(request);
		assertEquals(LOADED_RESULT, response.getPayload());
	}

	@Test
	public void findConfigurations() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.FIND);
		Message<?> response = callSyncGateway(request);
		assertEquals("[{\"name\":\"TestConfiguration\",\"stubbed\":false,\"state\":\"STARTING\",\"type\":\"JunitTestClassLoaderWrapper\",\"jdbcMigrator\":true}]", response.getPayload());
	}

	@Test
	public void findTestConfiguration() {
		getConfiguration().setVersion("dummy123");
		try {
			MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.CONFIGURATION, BusAction.FIND);
			request.setHeader("configuration", TestConfiguration.TEST_CONFIGURATION_NAME);
			Message<?> response = callSyncGateway(request);
			assertEquals("[{\"name\":\"TestConfiguration\",\"version\":\"dummy123\",\"stubbed\":false,\"state\":\"STARTING\",\"type\":\"JunitTestClassLoaderWrapper\",\"jdbcMigrator\":true}]", response.getPayload());
		} finally {
			getConfiguration().setVersion(null);
		}
	}
}
