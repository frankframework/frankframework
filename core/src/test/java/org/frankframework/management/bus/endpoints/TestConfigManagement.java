package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestConfiguration;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
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
