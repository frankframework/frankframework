package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.logging.IbisMaskingLayout;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.util.AppConstants;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
public class TestUpdateLogSettings extends BusTestBase {

	@Test
	@WithMockUser(roles = { "IbisTester" })
	public void getLogSettings() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_CONFIGURATION, BusAction.GET);
		Message<?> response = callSyncGateway(request);
		String json = (String) response.getPayload();

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> responseMap = mapper.readValue(json, Map.class);

		assertEquals(-1, responseMap.get("maxMessageLength"));
	}

	@Test
	@WithMockUser(roles = { "IbisTester" })
	public void updateLogDefinitionsNoHeader() {
		assertEquals(-1, IbisMaskingLayout.getMaxLength(), "maxLength should default to -1 (off)");
		boolean logIntermediary = AppConstants.getInstance().getBoolean("log.logIntermediaryResults", true);
		assertTrue(logIntermediary);

		try {
			MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_CONFIGURATION, BusAction.MANAGE);
			request.setHeader("logLevel", "");
			request.setHeader("logIntermediaryResults", !logIntermediary);
			request.setHeader("maxMessageLength", 10240);
			callAsyncGateway(request);

			assertEquals(10240, IbisMaskingLayout.getMaxLength(), "maxLength should be 10240");
			assertFalse(Boolean.parseBoolean(AppConstants.getInstance().get("log.logIntermediaryResults")));
		} finally {
			IbisMaskingLayout.setMaxLength(-1);
			AppConstants.getInstance().setProperty("log.logIntermediaryResults", true);
		}
	}
}
