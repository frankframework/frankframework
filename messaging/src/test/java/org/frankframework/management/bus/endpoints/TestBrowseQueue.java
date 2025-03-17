package org.frankframework.management.bus.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import com.mockrunner.mock.jms.MockMessage;

import org.frankframework.jms.JMSFacade.DestinationType;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.jms.JmsTransactionalStorage;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.mock.MockRunnerConnectionFactoryFactory;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestBrowseQueue extends BusTestBase {
	protected MockRunnerConnectionFactoryFactory mockConnectionFactoryFactory;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		mockConnectionFactoryFactory = getParentContext().getBean(MockRunnerConnectionFactoryFactory.class);
		JmsRealmFactory.getInstance().clear();
		JmsRealm jmsRealm = new JmsRealm();
		jmsRealm.setRealmName("dummyQCFAddedViaJmsRealm");
		jmsRealm.setQueueConnectionFactoryName("dummyQCFAddedViaJmsRealm");
		JmsRealmFactory.getInstance().addJmsRealm(jmsRealm);
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
