package org.frankframework.management.bus.endpoints;

import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(initializers = {SpringRootInitializer.class})
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
	@WithMockUser(authorities = { "ROLE_IbisTester" })
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
