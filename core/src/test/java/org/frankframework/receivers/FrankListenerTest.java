package org.frankframework.receivers;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineSession;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.SpringUtils;

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
	void setUp() throws ConfigurationException {
		configuration = new TestConfiguration(false);
		adapter = configuration.createBean();
		adapter.setName(ADAPTER_NAME);
		Receiver<Message> stupidMockitoReifiedReceiver = SpringUtils.createBean(adapter);
		receiver = spy(stupidMockitoReifiedReceiver);
		adapter.addReceiver(receiver);
		listener = SpringUtils.createBean(adapter);
		receiver.setListener(listener);
		listener.setName(LISTENER_NAME);
		adapter.setPipeLine(mock(PipeLine.class));
		adapter.configure();

		message = new Message("Tada");
		session = new PipeLineSession();
		session.put(PipeLineSession.CORRELATION_ID_KEY, "cid");
		session.put(PipeLineSession.MESSAGE_ID_KEY, "mid");
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session, configuration);
		if (listener != null) listener.stop();
	}

	@Test
	void getPhysicalDestinationName() throws ConfigurationException {
		// Arrange
		listener.setName(LISTENER_NAME);
		listener.configure();

		// Act
		String result = listener.getPhysicalDestinationName();

		// Assert
		assertEquals(configuration.getName() + "/" + LISTENER_NAME, result);
	}

	@Test
	void wrapRawMessage() throws ConfigurationException {
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
	void configure(String listenerName, String expected) throws ConfigurationException {
		// Arrange
		listener.setName(listenerName);

		// Act
		listener.configure();

		// Assert
		assertEquals(expected, listener.getName());
	}

	@Test
	void startSuccess() throws ConfigurationException {
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
	void openAlreadyStart() throws ConfigurationException {
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
	void startAlreadyRegistered() throws ConfigurationException {
		// Arrange
		FrankListener otherListener = SpringUtils.createBean(adapter);
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
	void stop() throws ConfigurationException {
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
	void extractMessage() throws ConfigurationException {
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
		adapter.start();

		await().pollInterval(5, TimeUnit.SECONDS)
				.atMost(Duration.ofMinutes(5))
				.until(adapter::isRunning);

		doReturn(message).when(receiver).processRawMessage(eq(listener), any(), eq(session), anyBoolean());

		// Act
		Message result = listener.processRequest(message, session);

		// Assert
		assertSame(message, result);
		verify(receiver).processRawMessage(eq(listener), any(), eq(session), anyBoolean());
	}

	@Test
	void processRequestWhenNotStart() throws ConfigurationException {
		// Arrange
		listener.configure();

		// Act
		assertThrows(ListenerException.class, () -> listener.processRequest(message, session));
	}
}
