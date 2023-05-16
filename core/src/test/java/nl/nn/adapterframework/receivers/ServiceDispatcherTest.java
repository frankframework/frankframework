package nl.nn.adapterframework.receivers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.stream.Message;

public class ServiceDispatcherTest {

	private final ServiceDispatcher serviceDispatcher = ServiceDispatcher.getInstance();

	@Before
	public void setUp() throws Exception {
		// Add in non-alphabetical order for testing getListenerNames()

		// TEST-CLIENT-2 will always return a NULL message
		serviceDispatcher.registerServiceClient("TEST-CLIENT-2",
			((correlationId, message, session) -> null));

		// TEST-CLIENT-1 will echo the message
		serviceDispatcher.registerServiceClient("TEST-CLIENT-1",
			((correlationId, message, session) -> message));
	}

	@After
	public void tearDown() {
		serviceDispatcher
			.getRegisteredListenerNames()
			.forEachRemaining((serviceName) -> serviceDispatcher.unregisterServiceClient(serviceName));
	}

	@Test
	public void dispatchRequestStringNotNull() throws ListenerException {
		// Act
		String result = serviceDispatcher.dispatchRequest("TEST-CLIENT-1", "cid", "MESSAGE", null);

		// Assert
		assertEquals("MESSAGE", result);
	}

	@Test
	public void dispatchRequestStringNull() throws ListenerException {
		// Act
		String result = serviceDispatcher.dispatchRequest("TEST-CLIENT-2", "cid", "MESSAGE", null);

		// Assert
		assertNull("Expected result for TEST-CLIENT-2 to be NULL", result);
	}

	@Test
	public void testDispatchRequestMessageNotNull() throws ListenerException, IOException {
		// Arrange
		Message message = new Message("MESSAGE");

		// Act
		Message result = serviceDispatcher.dispatchRequest("TEST-CLIENT-1", "cid", message, null);

		// Assert
		assertEquals("MESSAGE", result.asString());
	}

	@Test
	public void testDispatchRequestMessageNull() throws ListenerException, IOException {
		// Arrange
		Message message = new Message("MESSAGE");

		// Act
		Message result = serviceDispatcher.dispatchRequest("TEST-CLIENT-2", "cid", message, null);

		// Assert
		assertNull("Expected result for TEST-CLIENT-2 to be NULL", result);
	}

	@Test
	public void dispatchRequestServiceNotRegistered() throws ListenerException {
		// Act / Assert
		assertThrows(ListenerException.class, () -> serviceDispatcher.dispatchRequest("TEST-CLIENT-3", "cid", "MESSAGE", null));
	}


	@Test
	public void getRegisteredListenerNames() {
		// Act
		Iterator<String> listenerNames = serviceDispatcher.getRegisteredListenerNames();

		// Assert
		assertEquals("TEST-CLIENT-1", listenerNames.next());
		assertEquals("TEST-CLIENT-2", listenerNames.next());
		assertFalse("Was only expecting to have 2 listener names, got more", listenerNames.hasNext());
	}

	@Test
	public void isRegisteredServiceListener() {
		// Act / Assert
		assertTrue("Expected to have service-name [TEST-CLIENT-1]", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"));
		assertFalse("Expected not to have service-name [TEST-CLIENT-3]", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"));
	}

	@Test
	public void registerServiceClient() throws ListenerException {
		// Arrange
		assertFalse("Expected not to have service-name [TEST-CLIENT-3] before registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"));

		// Act
		serviceDispatcher.registerServiceClient("TEST-CLIENT-3", ((correlationId, message, session) -> null));

		// Assert
		assertTrue("Expected to have service-name [TEST-CLIENT-3] after registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"));
	}

	@Test
	public void registerServiceClientWhichAlreadyExists() throws ListenerException {
		// Arrange
		assertTrue("Expected to have service-name [TEST-CLIENT-1] before registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"));

		// Act
		// This should not throw exception
		serviceDispatcher.registerServiceClient("TEST-CLIENT-1", ((correlationId, message, session) -> null));

		// Assert
		assertTrue("Expected to have service-name [TEST-CLIENT-1] after registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"));
	}

	@Test
	public void unregisterServiceClient() {
		// Arrange
		assertTrue("Expected to have service-name [TEST-CLIENT-1] before registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"));

		// Act
		serviceDispatcher.unregisterServiceClient("TEST-CLIENT-1");

		// Assert
		assertFalse("Expected not to have service-name [TEST-CLIENT-1] after registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"));
	}

	@Test
	public void unregisterServiceClientWhichDoesntExist() {
		// Arrange
		assertFalse("Expected not to have service-name [TEST-CLIENT-3] before registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"));

		// Act
		serviceDispatcher.unregisterServiceClient("TEST-CLIENT-3");

		// Assert
		assertFalse("Expected not to have service-name [TEST-CLIENT-3] after registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-3"));

		// Make sure that others still exist
		assertTrue("Expected to have service-name [TEST-CLIENT-1] after registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-1"));
		assertTrue("Expected to have service-name [TEST-CLIENT-2] after registration", serviceDispatcher.isRegisteredServiceListener("TEST-CLIENT-2"));
	}

	@Test
	public void getListener() {
		// Act
		ServiceClient listener = serviceDispatcher.getListener("TEST-CLIENT-1");

		// Assert
		assertNotNull(listener);
	}
}
