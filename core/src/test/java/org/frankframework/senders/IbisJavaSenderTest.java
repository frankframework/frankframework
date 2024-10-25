package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;

public class IbisJavaSenderTest {

	public static final String ECHO_SERVICE = "ECHO-SERVICE";
	public static final String VALUE_FROM_CONTEXT_SERVICE = "VALUE_FROM_CONTEXT_SERVICE";

	private IbisJavaSender ibisJavaSender;
	private PipeLineSession pipeLineSession;

	@BeforeEach
	public void setUp() throws Exception {

		pipeLineSession = new PipeLineSession();

		ibisJavaSender = new IbisJavaSender();

		DispatcherManagerFactory.getDispatcherManager().register(ECHO_SERVICE, ((correlationId, message, requestContext) -> message));
		DispatcherManagerFactory.getDispatcherManager().register(VALUE_FROM_CONTEXT_SERVICE, ((correlationId, message, requestContext) -> requestContext.get(message).toString()));
	}

	@AfterEach
	public void tearDown() throws Exception {
		DispatcherManagerFactory.getDispatcherManager().unregister(ECHO_SERVICE);
		DispatcherManagerFactory.getDispatcherManager().unregister(VALUE_FROM_CONTEXT_SERVICE);
	}

	@Test
	public void sendMessage() throws SenderException, TimeoutException, IOException {
		// Arrange
		Message message = new Message("MESSAGE");
		ibisJavaSender.setServiceName(ECHO_SERVICE);

		// Act
		SenderResult result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("MESSAGE", result.getResult().asString());
	}

	@Test
	public void sendMessageWithParams() throws SenderException, TimeoutException, IOException, ConfigurationException {
		// Arrange
		Message message = new Message("my-parameter");
		ibisJavaSender.setServiceName(VALUE_FROM_CONTEXT_SERVICE);
		Parameter parameter = new Parameter("my-parameter", null);
		parameter.setSessionKey("my-parameter");
		parameter.configure();
		ibisJavaSender.addParameter(parameter);

		pipeLineSession.put("my-parameter", "parameter-value");

		// Act
		SenderResult result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("parameter-value", result.getResult().asString());
	}

	@Test
	public void sendMessageReturnContextToSession() throws SenderException, TimeoutException, IOException, ConfigurationException {
		// Arrange
		Message message = new Message("MESSAGE");
		ibisJavaSender.setServiceName(ECHO_SERVICE);
		ibisJavaSender.setReturnedSessionKeys("my-parameter,this-doesnt-exist");

		// Make use of the fact that parameters are copied to context so we can get them as return value
		Parameter parameter = new Parameter("my-parameter", "parameter-value");
		parameter.configure();
		ibisJavaSender.addParameter(parameter);

		// Act
		SenderResult result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("MESSAGE", result.getResult().asString());
		assertTrue(pipeLineSession.containsKey("my-parameter"), "After request the pipeline-session should contain key [my-parameter]");
		assertEquals("parameter-value", pipeLineSession.get("my-parameter"));
		assertTrue(pipeLineSession.containsKey("this-doesnt-exist"), "After request the pipeline-session should not contain key [this-doesnt-exist]");
		assertNull(pipeLineSession.get("this-doesnt-exist"), "Key not in return from service should have value [NULL]");
	}

	@Test
	public void sendMessageServiceNameFromSession() throws SenderException, TimeoutException, IOException {
		// Arrange
		Message message = new Message("MESSAGE");
		// Initialise with different service name as we will actually use
		ibisJavaSender.setServiceName(VALUE_FROM_CONTEXT_SERVICE);
		ibisJavaSender.setServiceNameSessionKey("service-name");

		pipeLineSession.put("service-name", ECHO_SERVICE);

		// Act
		SenderResult result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("MESSAGE", result.getResult().asString());
	}

	@Test
	public void testConfigure() {
		// Act / Assert
		assertThrows(ConfigurationException.class, ()-> ibisJavaSender.configure());
	}
}
