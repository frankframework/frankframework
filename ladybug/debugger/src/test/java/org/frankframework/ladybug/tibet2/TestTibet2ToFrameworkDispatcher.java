package org.frankframework.ladybug.tibet2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.wearefrank.ladybug.storage.StorageException;

import org.frankframework.management.bus.BusException;
import org.frankframework.management.bus.message.StringMessage;

@ContextConfiguration(classes = {TestBusConfiguration.class})
@ExtendWith(SpringExtension.class)
public class TestTibet2ToFrameworkDispatcher {

	@Autowired
	protected SpringUnitTestLocalGateway outputGateway;

	@Autowired
	private Tibet2ToFrameworkDispatcher dispatcher;

	@AfterEach
	public void afterEach() {
		Mockito.reset(outputGateway);
	}

	@ParameterizedTest
	@CsvSource({"success, Allowed"})
	@CsvSource({", Not allowed. Result of adapter AuthorisationCheck: unused"})
	@CsvSource({"rejected, Not allowed. Result of adapter AuthorisationCheck: unused"})
	@CsvSource({"error, Not allowed. Result of adapter AuthorisationCheck: unused"})
	@SuppressWarnings("unchecked")
	public void authorisationCheck(String responseState, String authResult) {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameter actually gets sent to the outputGateway
			assertEquals("main", headers.get("meta-configuration"));
			assertEquals("AuthorisationCheck", headers.get("meta-adapter"));
			assertEquals("TEST_PIPELINE", headers.get("topic"));
			assertEquals("UPLOAD", headers.get("action"));

			assertEquals("""
					[{"key":"StorageId","value":"1"},{"key":"View","value":"viewName"}]
					""".trim(), headers.get("meta-sessionKeys"));

			StringMessage response = new StringMessage("unused");
			response.setHeader("state", responseState);
			return response;
		});

		String result = dispatcher.authorisationCheck("1", "viewName");
		assertEquals(authResult, result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void authorisationCheckTechnicalError() throws StorageException {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenThrow(new BusException("technical-error-here"));

		String result = dispatcher.authorisationCheck("1", "viewName");
		assertEquals("Not allowed. Result of adapter AuthorisationCheck: technical-error-here", result);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void deleteReportSuccess() throws StorageException {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameter actually gets sent to the outputGateway
			assertEquals("main", headers.get("meta-configuration"));
			assertEquals("DeleteFromExceptionLog", headers.get("meta-adapter"));
			assertEquals("TEST_PIPELINE", headers.get("topic"));
			assertEquals("UPLOAD", headers.get("action"));

			return new StringMessage("<ok/>");
		});

		dispatcher.deleteReport("no-idea");
	}

	@Test
	@SuppressWarnings("unchecked")
	public void deleteReportFunctionalError() throws StorageException {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenAnswer(i -> {
			Message<String> msg = i.getArgument(0);
			MessageHeaders headers = msg.getHeaders();

			// assert that the parameter actually gets sent to the outputGateway
			assertEquals("main", headers.get("meta-configuration"));
			assertEquals("DeleteFromExceptionLog", headers.get("meta-adapter"));
			assertEquals("TEST_PIPELINE", headers.get("topic"));
			assertEquals("UPLOAD", headers.get("action"));

			return new StringMessage("functional-error-here");
		});

		StorageException ex = assertThrows(StorageException.class, () -> dispatcher.deleteReport("no-idea"));
		assertEquals("Delete failed: functional-error-here", ex.getMessage());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void deleteReportTechnicalError() throws StorageException {
		Mockito.when(outputGateway.sendSyncMessage(Mockito.any(Message.class))).thenThrow(new BusException("technical-error-here"));

		StorageException ex = assertThrows(StorageException.class, () -> dispatcher.deleteReport("no-idea"));
		assertEquals("Delete failed (see logging for more details)", ex.getMessage());
	}
}
