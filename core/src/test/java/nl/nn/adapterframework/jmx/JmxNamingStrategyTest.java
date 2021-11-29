package nl.nn.adapterframework.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.management.ObjectName;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;

public class JmxNamingStrategyTest {
	private JmxNamingStrategy namingStrategy;

	@Before
	public void setUp() {
		namingStrategy = new JmxNamingStrategy();
		namingStrategy.setDefaultDomain("defaultDomain");
	}

	@Test
	public void testGenericClass() throws Exception {
		ObjectName name = namingStrategy.getObjectName(this, null);
		assertEquals("JmxNamingStrategyTest", name.getKeyProperty("type")); //this defaults to the className
		assertEquals("nl.nn.adapterframework.jmx", name.getDomain()); //for non Adapters this defaults to the package name
	}

	@Test
	public void testAdapter() throws Exception {
		IAdapter adapter = new Adapter();
		adapter.setName("GenericAdapter"); //adapters should always have a name otherwise it wont be instantiated by the AdapterService

		ObjectName name = namingStrategy.getObjectName(adapter, null);
		assertNull("an adapter without configuration should not exist", name.getKeyProperty("type"));
		assertEquals("GenericAdapter", name.getKeyProperty("name")); //this defaults to the className
		assertEquals("defaultDomain", name.getDomain()); //for non Adapters this defaults to the package name
	}

	@Test
	public void testAdapterWithAStupidName() throws Exception {
		IAdapter adapter = new Adapter();
		adapter.setName("H#llo =; [i]<3u, \"have an adapter N4m3 :)"); //adapters should always have a name otherwise it wont be instantiated by the AdapterService

		ObjectName name = namingStrategy.getObjectName(adapter, null);
		assertNull("an adapter without configuration should not exist", name.getKeyProperty("type"));
		assertEquals("H#llo _; [i]<3u_ \"have an adapter N4m3 _)", name.getKeyProperty("name")); //this defaults to the className
		assertEquals("defaultDomain", name.getDomain()); //for non Adapters this defaults to the package name
	}

	@Test
	public void testAdapterWithConfiguration() throws Exception {
		IAdapter adapter = new Adapter();
		adapter.setName("GenericAdapter"); //adapters should always have a name otherwise it wont be instantiated by the AdapterService
		Configuration configuration = new Configuration();
		configuration.setName("SuperAwesomeConfiguration:3");
		configuration.setVersion("tralala build#123_09:15:59");
		adapter.setConfiguration(configuration);

		ObjectName name = namingStrategy.getObjectName(adapter, null);
		assertEquals("SuperAwesomeConfiguration_3-tralala build#123_09_15_59", name.getKeyProperty("type"));
		assertEquals("GenericAdapter", name.getKeyProperty("name")); //this defaults to the className
		assertEquals("defaultDomain", name.getDomain()); //for non Adapters this defaults to the package name
		assertNotNull(name.getKeyProperty("type"));
	}
}
