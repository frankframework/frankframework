package org.frankframework.management.bus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.SenderException;
import org.frankframework.management.bus.BusTestEndpoints.ExceptionTestTypes;
import org.frankframework.testutil.SpringRootInitializer;

@Log4j2
@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
public class TestSpringBusExceptionHandling extends BusTestBase {
	private static final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

	/**
	 * Clean up the Spring SecurityContext before all tests.
	 */
	@BeforeAll
	static void beforeAll() {
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		securityContextHolderStrategy.setContext(context);
	}

	@Test
	public void testEndpointMessageException() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.MESSAGE.name());

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		log.debug("testEndpointMessageException exception", e);

		// Assert
		assertInstanceOf(BusException.class, e.getCause());
		assertEquals("message without cause", e.getCause().getMessage());
	}

	@Test
	public void testEndpointNotFoundException() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.NOT_FOUND.name());

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		log.debug("testEndpointNotFoundException exception", e);

		// Assert
		assertInstanceOf(BusException.class, e.getCause());
		assertEquals("Resource not found", e.getCause().getMessage());
		assertEquals(404, ((BusException)e.getCause()).getStatusCode());
	}

	@Test
	public void testEndpointMessageWithCauseException() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.MESSAGE_WITH_CAUSE.name());

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		log.debug("testEndpointMessageWithCauseException exception", e);

		// Assert
		assertInstanceOf(BusException.class, e.getCause());
		assertEquals("message with a cause: cannot stream: cannot configure: (IllegalStateException) something is wrong", e.getCause().getMessage());
	}

	@Test
	public void testEndpointCauseException() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.WARNINGS);
		request.setHeader("type", ExceptionTestTypes.CAUSE.name());

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		log.debug("testEndpointCauseException exception", e);

		// Assert
		assertInstanceOf(SenderException.class, e.getCause());
		assertThat(e.getMessage(), Matchers.startsWith("error occurred during processing message in 'MethodInvokingMessageProcessor'"));
		assertThat(e.getCause().getMessage(), Matchers.endsWith("cannot stream: cannot configure: (IllegalStateException) something is wrong"));
	}

	@Test
	@WithMockUser(authorities = { "lala" })
	public void testEndpointMessageWithAuthorizationError() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		log.debug("testEndpointMessageWithAuthorizationError exception", e);

		// Assert
		assertInstanceOf(AccessDeniedException.class, e.getCause());
		assertEquals("Access Denied", e.getCause().getMessage());
	}

	@Test
	public void testEndpointMessageWithAuthenticationError() {
		// Arrange
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.DEBUG, BusAction.MANAGE);

		// Act
		MessageHandlingException e = assertThrows(MessageHandlingException.class, () -> callSyncGateway(request));
		log.debug("testEndpointMessageWithAuthenticationError exception", e);

		// Assert
		assertInstanceOf(AuthenticationException.class, e.getCause());
		assertEquals("An Authentication object was not found in the SecurityContext", e.getCause().getMessage());
	}
}
