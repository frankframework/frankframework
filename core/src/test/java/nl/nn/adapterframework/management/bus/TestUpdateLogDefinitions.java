package nl.nn.adapterframework.management.bus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;

import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * In the Log4J4Ibis.xml is a nl.nn.adapterframework.management.bus definition which we use here to test the BUS responses.
 */
@SuppressWarnings("unchecked") //can be removed once we implement a DAO
public class TestUpdateLogDefinitions extends BusTestBase {
	private static final String LOG_DEFINITION_PACKAGE = "nl.nn.adapterframework.management.bus";

	@Test
	public void getLogDefinitions() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.GET);
		Message<?> response = callSyncGateway(request);
		String json = (String) response.getPayload();

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> responseMap = mapper.readValue(json, Map.class);

		Object loggersObj = responseMap.get("loggers");
		if(loggersObj instanceof Map) {
			Map<String, Object> loggers = (Map<String, Object>) loggersObj;
			assertEquals("DEBUG", loggers.get(LOG_DEFINITION_PACKAGE));
		}

		boolean foundTestDefinition = false;
		Object definitionsObj = responseMap.get("definitions");
		if(definitionsObj instanceof List) {
			List<Map<String, ?>> definitions = (List<Map<String, ?>>) definitionsObj;
			for(Map<String, ?> definition : definitions) {
				if(LOG_DEFINITION_PACKAGE.equals(definition.get("name"))) {
					foundTestDefinition = true;
					assertEquals("DEBUG", definition.get("level"));
				}
			}
		}

		assertTrue("not testing the log definition retrieval!", foundTestDefinition);
	}

	@Test
	public void getLogDefinitionsWithFilter() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.GET);
		request.setHeader("filter", LOG_DEFINITION_PACKAGE);
		Message<?> response = callSyncGateway(request);
		String json = (String) response.getPayload();

		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> responseMap = mapper.readValue(json, Map.class);

		Object loggersObj = responseMap.get("loggers");
		if(loggersObj instanceof Map) {
			Map<String, String> loggers = (Map<String, String>) loggersObj;
			assertEquals("DEBUG", loggers.get("nl.nn.adapterframework.management.bus.endpoints"));
		}

		String expectedJson = TestFileUtils.getTestFile("/Management/logDefinitionsWithBusFilter.json");
		MatchUtils.assertJsonEquals(expectedJson, json);
	}

	@Test
	public void reconfigureLogDefinitionsNoHeaders() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		Message<?> response = callSyncGateway(request);

		assertEquals(400, response.getHeaders().get(ResponseMessage.STATUS_KEY));
	}

	@Test
	public void reconfigureLogDefinitionsEmptyHeader() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("reconfigure", "");
		Message<?> response = callSyncGateway(request);

		assertEquals(204, response.getHeaders().get(ResponseMessage.STATUS_KEY));
	}

	@Test
	public void reconfigureLogDefinitions() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("reconfigure", "true");
		Message<?> response = callSyncGateway(request);

		assertEquals(202, response.getHeaders().get(ResponseMessage.STATUS_KEY));
	}

	@Test
	public void updateLogDefinitionsEmptyHeader() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);
		request.setHeader("level", "");
		Message<?> response = callSyncGateway(request);

		assertEquals(400, response.getHeaders().get(ResponseMessage.STATUS_KEY));
	}

	@Test
	public void updateLogDefinitionsNoHeader() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);
		Message<?> response = callSyncGateway(request);

		assertEquals(400, response.getHeaders().get(ResponseMessage.STATUS_KEY));
	}

	@Test
	public void updateLogDefinitions() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);
		request.setHeader("level", "debug");
		Message<?> response = callSyncGateway(request);

		assertEquals(202, response.getHeaders().get(ResponseMessage.STATUS_KEY));
	}
}
