package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.jdbc.datasource.DataSourceFactory;
import org.frankframework.jdbc.datasource.TransactionalDbmsSupportAwareDataSourceProxy;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.FindAvailableDataSources.TestDatasource;
import org.frankframework.util.SpringUtils;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
public class TestSecurityItems extends BusTestBase {

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		JmsRealmFactory.getInstance().clear();
		JmsRealm jdbcRealm = new JmsRealm();
		jdbcRealm.setRealmName("dummyJmsRealm1");
		jdbcRealm.setDatasourceName("dummyDatasourceName");
		JmsRealmFactory.getInstance().addJmsRealm(jdbcRealm);

		JmsRealm jmsRealm = new JmsRealm();
		jmsRealm.setRealmName("dummyJmsRealm2");
		jmsRealm.setQueueConnectionFactoryName("dummyQCF");
		JmsRealmFactory.getInstance().addJmsRealm(jmsRealm);

		DataSourceFactory dataSourceFactory = getConfiguration().getBean(DataSourceFactory.class);
		DataSource mockDataSource = Mockito.mock(TransactionalDbmsSupportAwareDataSourceProxy.class);
		dataSourceFactory.add(mockDataSource, TestDatasource.H2.getDataSourceName());

		// Strange hack because the endpoint uses the SecurityItems backend context.
		SpringUtils.registerSingleton(getConfiguration().getParent(), "mockDataSourceFactory", dataSourceFactory);
	}

	@Test
	@WithMockUser(authorities = { "ROLE_IbisTester" })
	public void getSecurityItems() throws Exception {
		DataSourceFactory dataSourceFactory = getConfiguration().getBean(DataSourceFactory.class);
		List<String> dataSources = dataSourceFactory.getDataSourceNames();
		assertEquals(1, dataSources.size()); // Ensure there is a datasource available
		assertEquals("[jdbc/H2]", dataSources.toString());

		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.SECURITY_ITEMS);
		request.setHeader("configuration", "testConfiguration");
		Message<?> response = callSyncGateway(request);
		String expectedJson = TestFileUtils.getTestFile("/Management/securityItemsResponse.json");
		String payload = (String) response.getPayload();
		payload = payload.replaceAll("hashCode: \\d+", "HASHCODE");
		payload = payload.replaceAll("(?:\"cyphers\":)\\[([\\r\\s\\t\\n\\w \",]+)+(:?\\])", "\"cyphers\":[\"IGNORE\"]");
		payload = payload.replaceAll("(?:\"protocols\":)\\[([\\r\\s\\t\\n\\w \",.]+)+(:?\\])", "\"protocols\":[\"IGNORE\"]");

		MatchUtils.assertJsonEquals(expectedJson, payload);
	}
}
