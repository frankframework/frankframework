package nl.nn.adapterframework.management.bus.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;

import com.mockrunner.mock.jms.MockMessage;

import nl.nn.adapterframework.jms.JMSFacade.DestinationType;
import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsTransactionalStorage;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.mock.MockRunnerConnectionFactoryFactory;

public class TestBrowseQueue extends BusTestBase {

	@Override
	@BeforeEach
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
		request.setHeader("connectionFactory", MockRunnerConnectionFactoryFactory.MOCK_CONNECTION_FACTORY_NAME);
		request.setHeader("destination", "testTable");
		request.setHeader("type", DestinationType.QUEUE.name());

		MockMessage mockMessage = new MockMessage();
		mockMessage.setJMSMessageID("dummyMessageId");
		mockMessage.setJMSCorrelationID("dummyCorrelationId");
		mockMessage.setStringProperty(JmsTransactionalStorage.FIELD_HOST, "dummy-hostname");
		mockMessage.setJMSExpiration(12L);
		mockMessage.setJMSTimestamp(45L);
		mockConnectionFactoryFactory.addMessageOnQueue("testTable", mockMessage);

		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/BrowseQueue.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}
}
