package nl.nn.adapterframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.nn.adapterframework.logging.IbisMaskingLayout;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.util.AppConstants;

public class TestUpdateLogSettings extends BusTestBase {

	@Test
	public void getLogSettings() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_CONFIGURATION, BusAction.GET);
		Message<?> response = callSyncGateway(request);
		String json = (String) response.getPayload();

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> responseMap = mapper.readValue(json, Map.class);

		assertEquals(-1, responseMap.get("maxMessageLength"));
	}

	@Test
	public void updateLogDefinitionsNoHeader() throws Exception {
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
