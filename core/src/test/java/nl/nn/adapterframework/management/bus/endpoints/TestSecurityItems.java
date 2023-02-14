package nl.nn.adapterframework.management.bus.endpoints;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import nl.nn.adapterframework.jms.JmsRealm;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.management.bus.BusTestBase;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.testutil.MatchUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder;

public class TestSecurityItems extends BusTestBase {

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
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
		mockFixedQuerySenderResult("select datasource from database", ResultSetBuilder.create().build());

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SECURITY_ITEMS);
		request.setHeader("configuration", "testConfiguration");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/securityItemsResponse.json");
		String payload = (String) response.getPayload();
		payload = payload.replaceAll("hashCode: \\d+", "HASHCODE");
		MatchUtils.assertJsonEquals(expectedJson, payload);
	}
}
