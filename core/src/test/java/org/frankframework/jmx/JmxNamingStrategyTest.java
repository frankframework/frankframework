package org.frankframework.jmx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import javax.management.ObjectName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.Configuration;
import org.frankframework.core.Adapter;

public class JmxNamingStrategyTest {
	private JmxNamingStrategy namingStrategy;

	@BeforeEach
	public void setUp() {
		namingStrategy = new JmxNamingStrategy();
		namingStrategy.setDefaultDomain("defaultDomain");
	}

	@Test
	public void testGenericClass() throws Exception {
		ObjectName name = namingStrategy.getObjectName(this, null);
		assertEquals("JmxNamingStrategyTest", name.getKeyProperty("type")); //this defaults to the className
		assertEquals("org.frankframework.jmx", name.getDomain()); //for non Adapters this defaults to the package name
	}

	@Test
	public void testAdapter() throws Exception {
		Adapter adapter = new Adapter();
		adapter.setName("GenericAdapter"); //adapters should always have a name otherwise it wont be instantiated by the AdapterService

		ObjectName name = namingStrategy.getObjectName(adapter, null);
		assertNull(name.getKeyProperty("type"), "an adapter without configuration should not exist");
		assertEquals("GenericAdapter", name.getKeyProperty("name")); //this defaults to the className
		assertEquals("defaultDomain", name.getDomain()); //for non Adapters this defaults to the package name
	}

	@Test
	public void testAdapterWithAStupidName() throws Exception {
		Adapter adapter = new Adapter();
		adapter.setName("H#llo =; [i]<3u, \"have an adapter N4m3 :)"); //adapters should always have a name otherwise it wont be instantiated by the AdapterService

		ObjectName name = namingStrategy.getObjectName(adapter, null);
		assertNull(name.getKeyProperty("type"), "an adapter without configuration should not exist");
		assertEquals("H#llo _; [i]<3u_ \"have an adapter N4m3 _)", name.getKeyProperty("name")); //this defaults to the className
		assertEquals("defaultDomain", name.getDomain()); //for non Adapters this defaults to the package name
	}

	@Test
	public void testAdapterWithConfiguration() throws Exception {
		Adapter adapter = new Adapter();
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
