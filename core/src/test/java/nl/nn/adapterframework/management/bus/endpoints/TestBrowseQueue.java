package nl.nn.adapterframework.management.bus.endpoints;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.mock.ConnectionFactoryFactoryMock;

public class TestBrowseQueue extends BusTestBase {

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		JmsRealmFactory.getInstance().clear();
		JmsRealm jmsRealm = new JmsRealm();
		jmsRealm.setRealmName("dummyQCFAddedViaJmsRealm");
		jmsRealm.setQueueConnectionFactoryName("dummyQCFAddedViaJmsRealm");
		JmsRealmFactory.getInstance().registerJmsRealm(jmsRealm);
	}

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
