package nl.nn.adapterframework.management.bus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import nl.nn.adapterframework.management.bus.BusTestEndpoints.ExceptionTestTypes;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.testutil.SpringRootInitializer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {SpringRootInitializer.class})
public class TestSpringBusExceptionHandling extends BusTestBase {

	@Test
	public void testEndpointMessageException() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.MESSAGE.name());

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));

		// Assert
		assertTrue(e.getCause() instanceof BusException);
		assertEquals("message without cause", e.getCause().getMessage());
	}

	@Test
	public void testEndpointMessageWithCauseException() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.MESSAGE_WITH_CAUSE.name());

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));

		// Assert
		assertTrue(e.getCause() instanceof BusException);
		assertEquals("message with a cause: cannot stream: cannot configure: (IllegalStateException) something is wrong", e.getCause().getMessage());
	}

	@Test
	public void testEndpointCauseException() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.CAUSE.name());

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));

		// Assert
		assertTrue(e.getCause() instanceof StreamingException);
		String message = e.getMessage();
		assertThat(message, Matchers.startsWith("error occurred during processing message in 'MethodInvokingMessageProcessor'"));
		assertThat(message, Matchers.endsWith("nested exception is nl.nn.adapterframework.stream.StreamingException: cannot stream: cannot configure: (IllegalStateException) something is wrong"));
	}

	@Test
	public void testEndpointMessageWithAuthenticationError() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));

		// Assert
		assertTrue(e.getCause() instanceof AuthenticationException);
		assertEquals("An Authentication object was not found in the SecurityContext", e.getCause().getMessage());
	}

	@Test
	@WithMockUser(authorities = { "lala" })
	public void testEndpointMessageWithAuthorizationError() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));

		// Assert
		assertTrue(e.getCause() instanceof AccessDeniedException);
		assertEquals("Access Denied", e.getCause().getMessage());
	}
}