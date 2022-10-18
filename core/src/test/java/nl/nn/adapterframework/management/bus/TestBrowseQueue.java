package nl.nn.adapterframework.management.bus;

import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.mock.ConnectionFactoryFactoryMock;

public class TestBrowseQueue extends BusTestBase {

	@Test
	public void getConnectionFactoriesTest() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.QUEUE, BusAction.GET);
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/getConnectionFactories.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}

	@Test
	public void testBrowseQueue() throws Exception {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.QUEUE, BusAction.FIND);
		request.setHeader("connectionFactory", ConnectionFactoryFactoryMock.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", "testTable");
		request.setHeader("type", DestinationType.QUEUE.name());

		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseQueue.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}
}
