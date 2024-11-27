package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.testutil.SpringRootInitializer;

/**
 * In the Log4J4Ibis.xml is a org.frankframework.management.bus definition which we use here to test the BUS responses.
 */
@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
@SuppressWarnings("unchecked") //can be removed once we implement a DAO
public class TestUpdateLogDefinitions extends BusTestBase {
	private static final String LOG_DEFINITION_PACKAGE = "org.frankframework.management.bus";
	private static final String EXCEPTION_MESSAGE = "neither [reconfigure], [logPackage] or [level] provided";
	private static final String NEW_LOG_DEFINITION_PACKAGE = "org.frankframework.core.Adapter";
	private static final String NEW_LOG_EXCEPTION_MESSAGE = "neither [logPackage] or [level] provided";

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

		assertTrue(foundTestDefinition, "not testing the log definition retrieval!");
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
			assertEquals("DEBUG", loggers.get("org.frankframework.management.bus.endpoints"));
		}

		assertThat(json, Matchers.containsString("\"org.frankframework.management.bus\":"));
		assertThat(json, Matchers.containsString("\"org.frankframework.management.bus.endpoints\":"));
	}

	@Test
	public void reconfigureLogDefinitionsNoHeaders() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);

		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals(EXCEPTION_MESSAGE, be.getMessage());
		}
	}

	@Test
	public void reconfigureLogDefinitionsEmptyHeader() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("reconfigure", "");
		assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
	}

	@Test
	public void reconfigureLogDefinitions() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("reconfigure", "true");
		Message<?> response = callSyncGateway(request);

		assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0));
	}

	@Test
	public void updateLogDefinitionsEmptyHeader() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);
		request.setHeader("level", "");

		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals(EXCEPTION_MESSAGE, be.getMessage());
		}
	}

	@Test
	public void updateLogDefinitionsNoHeader() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);

		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals(EXCEPTION_MESSAGE, be.getMessage());
		}
	}

	@Test
	public void updateLogDefinitions() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.MANAGE);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);
		request.setHeader("level", "debug");
		Message<?> response = callSyncGateway(request);

		assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0));
	}

	@Test
	public void createLogDefinitionEmptyHeader() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.UPLOAD);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);
		request.setHeader("level", "");

		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals(NEW_LOG_EXCEPTION_MESSAGE, be.getMessage());
		}
	}

	@Test
	public void createLogDefinitionNoHeader() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.UPLOAD);
		request.setHeader("logPackage", LOG_DEFINITION_PACKAGE);

		try {
			callSyncGateway(request);
		} catch (Exception e) {
			assertTrue(e.getCause() instanceof BusException);
			BusException be = (BusException) e.getCause();
			assertEquals(NEW_LOG_EXCEPTION_MESSAGE, be.getMessage());
		}
	}

	@Test
	public void createLogDefinition() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.LOG_DEFINITIONS, BusAction.UPLOAD);
		request.setHeader("logPackage", NEW_LOG_DEFINITION_PACKAGE);
		request.setHeader("level", "debug");
		Message<?> response = callSyncGateway(request);

		assertEquals(202, BusMessageUtils.getIntHeader(response, MessageBase.STATUS_KEY, 0));
	}
}
