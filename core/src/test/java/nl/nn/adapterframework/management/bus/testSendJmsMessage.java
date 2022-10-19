package nl.nn.adapterframework.management.bus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

import javax.jms.JMSException;
import javax.jms.Message;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;

import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.testutil.TestScopeProvider;
import nl.nn.adapterframework.testutil.mock.ConnectionFactoryFactoryMock;

public class testSendJmsMessage extends BusTestBase {

	@Test
	public void putMessageOnQueue() throws Exception {
		String payload = "dummy";
		MessageBuilder request = createRequestMessage(payload, BusTopic.QUEUE, BusAction.UPLOAD);
		request.setHeader("connectionFactory", ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", "dummyDestination");
		request.setHeader("type", DestinationType.QUEUE.name());
		callAsyncGateway(request);
		javax.jms.Message jmsResponse = ConnectionFactoryFactoryMock.getMessageHandler().receive();
		assertTrue(jmsResponse instanceof javax.jms.TextMessage);
		String responseMessage = ((javax.jms.TextMessage) jmsResponse).getText();
		System.out.println(responseMessage);
	}
}
