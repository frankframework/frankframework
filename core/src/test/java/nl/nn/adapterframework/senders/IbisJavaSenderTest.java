package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.dispatcher.DispatcherManagerFactory;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;

public class IbisJavaSenderTest {

	public static final String ECHO_SERVICE = "ECHO-SERVICE";
	public static final String VALUE_FROM_CONTEXT_SERVICE = "VALUE_FROM_CONTEXT_SERVICE";

	private IbisJavaSender ibisJavaSender;
	private PipeLineSession pipeLineSession;

	@Before
	public void setUp() throws Exception {

		pipeLineSession = new PipeLineSession();

		ibisJavaSender = new IbisJavaSender();

		DispatcherManagerFactory.getDispatcherManager().register(ECHO_SERVICE, ((correlationId, message, requestContext) -> message));
		DispatcherManagerFactory.getDispatcherManager().register(VALUE_FROM_CONTEXT_SERVICE, ((correlationId, message, requestContext) -> requestContext.get(message).toString()));
	}

	@After
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
		Message result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("MESSAGE", result.asString());
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
		Message result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("parameter-value", result.asString());
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
		Message result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("MESSAGE", result.asString());
		assertTrue("After request the pipeline-session should contain key [my-parameter]", pipeLineSession.containsKey("my-parameter"));
		assertEquals("parameter-value", pipeLineSession.get("my-parameter"));
		assertTrue("After request the pipeline-session should not contain key [this-doesnt-exist]", pipeLineSession.containsKey("this-doesnt-exist"));
		assertNull("Key not in return from service should have value [NULL]", pipeLineSession.get("this-doesnt-exist"));
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
		Message result = ibisJavaSender.sendMessage(message, pipeLineSession);

		// Assert
		assertEquals("MESSAGE", result.asString());
	}

	@Test
	public void testConfigure() {
		// Act / Assert
		assertThrows(ConfigurationException.class, ()-> ibisJavaSender.configure());
	}
}
