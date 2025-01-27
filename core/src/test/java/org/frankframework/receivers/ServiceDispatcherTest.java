package org.frankframework.receivers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.util.AssertionErrors.assertNull;

import java.io.IOException;
import java.util.Iterator;
import java.util.SortedSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.ListenerException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;

public class ServiceDispatcherTest {

	private final ServiceDispatcher serviceDispatcher = ServiceDispatcher.getInstance();

	@BeforeEach
	public void setUp() throws Exception {
		// Add in non-alphabetical order for testing getListenerNames()

		// TEST-CLIENT-2 will always return a NULL message
		serviceDispatcher.registerServiceClient("TEST-CLIENT-2",
			((message, session) -> null));

		// TEST-CLIENT-1 will echo the message
		serviceDispatcher.registerServiceClient("TEST-CLIENT-1",
			((message, session) -> message));
	}

	@AfterEach
	public void tearDown() {
		serviceDispatcher
			.getRegisteredListenerNames()
			.forEach(serviceDispatcher::unregisterServiceClient);
	}

	@Test
	public void testDispatchRequestMessageNotNull() throws ListenerException, IOException {
		// Arrange
		Message message = new Message("MESSAGE");

		// Act
		Message result = serviceDispatcher.dispatchRequest("TEST-CLIENT-1", message, null);

		// Assert
		assertEquals("MESSAGE", result.asString());
	}

	@Test
	public void testDispatchRequestMessageNull() throws ListenerException {
		// Arrange
		Message message = new Message("MESSAGE");

		// Act
		Message result = serviceDispatcher.dispatchRequest("TEST-CLIENT-2", message, null);

		// Assert
		assertNull("Expected result for TEST-CLIENT-2 to be NULL", result);
	}

	@Test
	public void dispatchRequestServiceNotRegistered() throws ListenerException {
		// Arrange
		Message message = new Message("MESSAGE");

		// Act / Assert
		assertThrows(ListenerException.class, () -> serviceDispatcher.dispatchRequest("TEST-CLIENT-3", message, null));
	}


	@Test
	public void getRegisteredListenerNames() {
		// Act
		SortedSet<String> listenerNames = serviceDispatcher.getRegisteredListenerNames();

		// Assert
		Iterator<String> listenerNamesIter = listenerNames.iterator();
		assertEquals("TEST-CLIENT-1", listenerNamesIter.next());
		assertEquals("TEST-CLIENT-2", listenerNamesIter.next());
		assertFalse(listenerNamesIter.hasNext(), "Was only expecting to have 2 listener names, got more");
	}

	@Test
	public void isRegisteredServiceListener() {
		// Act / Assert
		assertTrue(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"), "Expected to have service-name [TEST-CLIENT-1]");
		assertFalse(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"), "Expected not to have service-name [TEST-CLIENT-3]");
	}

	@Test
	public void registerServiceClient() throws ListenerException {
		// Arrange
		assertFalse(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"), "Expected not to have service-name [TEST-CLIENT-3] before registration");

		// Act
		serviceDispatcher.registerServiceClient("TEST-CLIENT-3", ((message, session) -> null));

		// Assert
		assertTrue(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"), "Expected to have service-name [TEST-CLIENT-3] after registration");
	}

	@Test
	public void registerServiceClientWhichAlreadyExists() {
		// Arrange
		assertTrue(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"), "Expected to have service-name [TEST-CLIENT-1] before registration");

		// Act
		// This should not throw exception
		assertThrows(LifecycleException.class, ()->serviceDispatcher.registerServiceClient("TEST-CLIENT-1", ((message, session) -> null)));

		// Assert
		assertTrue(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"), "Expected to have service-name [TEST-CLIENT-1] after registration");
	}

	@Test
	public void unregisterServiceClient() {
		// Arrange
		assertTrue(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"), "Expected to have service-name [TEST-CLIENT-1] before registration");

		// Act
		serviceDispatcher.unregisterServiceClient("TEST-CLIENT-1");

		// Assert
		assertFalse(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"), "Expected not to have service-name [TEST-CLIENT-1] after registration");
	}

	@Test
	public void unregisterServiceClientWhichDoesntExist() {
		// Arrange
		assertFalse(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"), "Expected not to have service-name [TEST-CLIENT-3] before registration");

		// Act
		serviceDispatcher.unregisterServiceClient("TEST-CLIENT-3");

		// Assert
		assertFalse(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"), "Expected not to have service-name [TEST-CLIENT-3] after registration");

		// Make sure that others still exist
		assertTrue(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"), "Expected to have service-name [TEST-CLIENT-1] after registration");
		assertTrue(serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-2"), "Expected to have service-name [TEST-CLIENT-2] after registration");
	}

	@Test
	public void getListener() {
		// Act
		ServiceClient listener = serviceDispatcher.getListener("TEST-CLIENT-1");

		// Assert
		assertNotNull(listener);
	}
}
