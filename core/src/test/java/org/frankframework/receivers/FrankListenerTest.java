package org.frankframework.receivers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.core.Adapter;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;

class FrankListenerTest {
	private static final String ADAPTER_NAME = "The Adapter";
	private static final String LISTENER_NAME = "The Listener";

	TestConfiguration configuration;
	Adapter adapter;
	Receiver<Message> receiver;
	FrankListener listener;
	Message message;
	PipeLineSession session;

	@BeforeEach
	void setUp() {
		configuration = new TestConfiguration(false);
		adapter = mock();
		receiver = mock();
		when(receiver.getAdapter()).thenReturn(adapter);
		when(adapter.getName()).thenReturn(ADAPTER_NAME);
		listener = configuration.createBean();
		listener.setHandler(receiver);
		message = new Message("Tada");
		session = new PipeLineSession();
		session.put(PipeLineSession.CORRELATION_ID_KEY, "cid");
		session.put(PipeLineSession.MESSAGE_ID_KEY, "mid");
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(message, session, configuration);
		listener.stop();
	}

	@Test
	void getPhysicalDestinationName() {
		// Arrange
		listener.setName(LISTENER_NAME);
		listener.configure();

		// Act
		String result = listener.getPhysicalDestinationName();

		// Assert
		assertEquals(configuration.getName() + "/" + LISTENER_NAME, result);
	}

	@Test
	void wrapRawMessage() {
		// Arrange
		listener.configure();

		// Act
		MessageWrapper<Message> result = listener.wrapRawMessage(message, session);

		// Assert
		assertEquals(message, result.getMessage());
		assertEquals("cid", result.getCorrelationId());
		assertEquals("mid", result.getId());
	}

	@ParameterizedTest
	@CsvSource({"," + ADAPTER_NAME, LISTENER_NAME + "," + LISTENER_NAME})
	void configure(String listenerName, String expected) {
		// Arrange
		listener.setName(listenerName);

		// Act
		listener.configure();

		// Assert
		assertEquals(expected, listener.getName());
	}

	@Test
	void startSuccess() {
		// Arrange
		listener.configure();

		// Verify that listener is not open and cannot be found before opening
		assertFalse(listener.isOpen(), "Listener not supposed to be open before test");
		assertNull(FrankListener.getListener(listener.getPhysicalDestinationName()));

		// Act
		listener.start();

		// Assert
		// Verify that listener is now open and can be found
		assertTrue(listener.isOpen(), "Listener is supposed to be open after test");
		assertSame(listener, FrankListener.getListener(listener.getPhysicalDestinationName()));
	}

	@Test
	void openAlreadyStart() {
		// Arrange
		listener.configure();

		assertFalse(listener.isOpen(), "Listener not supposed to be open before test");
		listener.start();
		assertTrue(listener.isOpen(), "Listener is supposed to be open after test");

		// Act
		assertDoesNotThrow(listener::start);

		// Assert
		assertTrue(listener.isOpen(), "Listener is supposed to be open after test");

	}

	@Test
	void startAlreadyRegistered() {
		// Arrange
		FrankListener otherListener = configuration.createBean();
		otherListener.setHandler(receiver);
		otherListener.setName(LISTENER_NAME);
		otherListener.configure();
		listener.setName(LISTENER_NAME);
		listener.configure();

		assertDoesNotThrow(listener::start);

		// Act
		assertThrows(LifecycleException.class, otherListener::start);

		// Assert
		assertFalse(otherListener.isOpen(), "Other Listener is not supposed to be open after the test");
	}

	@Test
	void stop() {
		// Arrange
		listener.configure();
		listener.start();
		// Verify that listener is now open before closing it, and can be found.
		assertTrue(listener.isOpen(), "Listener is supposed to be open after test");
		assertSame(listener, FrankListener.getListener(listener.getPhysicalDestinationName()));

		// Act
		listener.stop();

		// Assert
		// Verify that listener is closed again, and can no longer be found
		assertFalse(listener.isOpen(), "Listener is supposed to be closed after test");
		assertNull(FrankListener.getListener(listener.getPhysicalDestinationName()));
	}

	@Test
	void extractMessage() {
		// Arrange
		RawMessageWrapper<Message> rawMessageWrapper = new RawMessageWrapper<>(message);
		listener.configure();

		// Act
		Message result = listener.extractMessage(rawMessageWrapper, session);

		// Assert
		assertSame(message, result);
	}

	@Test
	void processRequest() throws ListenerException {
		// Testing with real objects instead of mocks is done in the FrankSenderTest

		// Arrange
		listener.configure();
		listener.start();
		when(receiver.processRequest(any(), any(), any())).thenReturn(message);

		// Act
		Message result = listener.processRequest(message, session);

		// Assert
		assertSame(message, result);
		verify(receiver).processRequest(eq(listener), any(), eq(session));
	}

	@Test
	void processRequestWhenNotStart() {
		// Arrange
		listener.configure();

		// Act
		assertThrows(ListenerException.class, () -> listener.processRequest(message, session));
	}
}
