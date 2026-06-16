package org.frankframework.management.bus.endpoints;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.frankframework.core.Adapter;
import org.frankframework.management.bus.message.StringMessage;
import org.frankframework.receivers.Receiver;
import org.frankframework.testutil.MatchUtils;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.TxManagerTest;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.SpringUtils;

/**
 * Unlike the other test classes in this package, this one doesn't use the message-bus.
 * Here we test the underlying database class.
 */
public class TestIbisstoreSummary {

	@TxManagerTest
	@WithLiquibase
	@WithLiquibase(file = "/Migrator/PrefillErrorStore.xml")
	public void testBrowseTableWithoutTablename(DatabaseTestEnvironment env) throws IOException {
		IbisstoreSummary instance = spy(IbisstoreSummary.class);
		List<Adapter> adapters = getTestAdapters(env.getConfiguration());
		doReturn(adapters).when(instance).getAdapters();

		env.autowire(instance);

		StringMessage result = instance.execute(env.getDataSourceName(), null);
		String expected = TestFileUtils.getTestFile("/Management/TestIbisstoreSummary.json");
		MatchUtils.assertJsonEquals(expected, result.getPayload());
	}

	private List<Adapter> getTestAdapters(TestConfiguration config) {
		Adapter adapter = config.createBean();
		adapter.setName("myAdapter");
		Receiver<?> receiver = SpringUtils.createBean(adapter, Receiver.class);
		receiver.setName("myReceiver");
		adapter.addReceiver(receiver);

		return Collections.singletonList(adapter);
	}
}
