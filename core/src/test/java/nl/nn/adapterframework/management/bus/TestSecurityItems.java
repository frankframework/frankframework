package nl.nn.adapterframework.management.bus;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.testutil.FixedQuerySenderMock.ResultSetBuilder;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class TestSecurityItems extends BusTestBase {

	@Before
	public void setUp() {
		JmsRealmFactory.getInstance().clear();
		JmsRealm jdbcRealm = new JmsRealm();
		jdbcRealm.setRealmName("dummyJmsRealm1");
		jdbcRealm.setDatasourceName("dummyDatasourceName");
		JmsRealmFactory.getInstance().registerJmsRealm(jdbcRealm);

		JmsRealm jmsRealm = new JmsRealm();
		jmsRealm.setRealmName("dummyJmsRealm2");
		jmsRealm.setQueueConnectionFactoryName("dummyQCF");
		JmsRealmFactory.getInstance().registerJmsRealm(jmsRealm);
	}

	@Test
	public void getSecurityItems() throws Exception {
		mockQuery("select datasource from database", ResultSetBuilder.create().build());

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SECURITY_ITEMS);
		request.setHeader("configuration", "testConfiguration");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/securityItemsResponse.json");
		MatchUtils.assertJsonEquals(expectedJson, (String) response.getPayload());
	}
}
