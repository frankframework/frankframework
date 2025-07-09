package org.frankframework.management.bus.endpoints;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.jdbc.datasource.DataSourceFactory;
import org.frankframework.jdbc.datasource.TransactionalDbmsSupportAwareDataSourceProxy;
import org.frankframework.jms.JmsRealm;
import org.frankframework.jms.JmsRealmFactory;
import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.pipes.SignaturePipe;
import org.frankframework.testutil.FindAvailableDataSources.TestDatasource;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.TimeProvider;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
public class TestSecurityItems extends BusTestBase {

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
		ZonedDateTime testTime = ZonedDateTime.of(2025, 6, 15, 10, 0, 0, 0, ZoneId.systemDefault());
		TimeProvider.clock = Clock.fixed(testTime.toInstant(), ZoneId.systemDefault());

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

		Adapter adapter = SpringUtils.createBean(getConfiguration());
		adapter.setName("myAdapter");
		PipeLine pipeline = SpringUtils.createBean(adapter);
		SignaturePipe pipe = SpringUtils.createBean(adapter);
		pipe.setKeystore("Encryption/common_name.p12");
		pipe.setKeystoreType(KeystoreType.PKCS12);
		pipe.setKeystorePassword("changeit");
		pipe.setName("signaturePipe");
		pipeline.addPipe(pipe);
		adapter.setPipeLine(pipeline);
		getConfiguration().addAdapter(adapter);
	}

	@AfterEach
	public void tearDown() {
		TimeProvider.clock = Clock.systemUTC();
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
		String payload = (String) response.getPayload();
		payload = payload.replaceAll("hashCode: \\d+", "HASHCODE");
		payload = payload.replaceAll("(?:\"cyphers\":)\\[([\\r\\s\\t\\n\\w \",]+)+(:?\\])", "\"cyphers\":[\"IGNORE\"]");
		payload = payload.replaceAll("(?:\"protocols\":)\\[([\\r\\s\\t\\n\\w \",.]+)+(:?\\])", "\"protocols\":[\"IGNORE\"]");
		payload = payload.replaceAll("JmsRealm:.+?,", "JmsRealm,");

		String expectedJson = TestFileUtils.getTestFile("/Management/securityItemsResponse.json");
		MatchUtils.assertJsonEquals(expectedJson, payload);
	}
}
