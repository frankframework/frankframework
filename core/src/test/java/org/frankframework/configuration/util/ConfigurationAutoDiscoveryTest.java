package org.frankframework.configuration.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.classloaders.DatabaseClassLoader;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.ScanningDirectoryClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.testutil.mock.FixedQuerySenderMock.ResultSetBuilder;

public class ConfigurationAutoDiscoveryTest {

	@DatabaseTest
	@WithLiquibase(file = "Migrator/CreateIbisConfig.xml")
	@WithLiquibase(file = "Migrator/SetupConfigsInDatabase.xml")
	public void retrieveConfigNamesFromDatabaseTest(DatabaseTestEnvironment env) throws Exception {
		TestConfiguration applicationContext = env.getConfiguration();
		ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();

		autoDiscovery.withDatabaseScanner(env.getDataSourceName());
		Set<String> configs = autoDiscovery.scan(false).keySet();

		assertThat(configs, IsIterableContainingInOrder.contains("config1", "config2", "config3", "config4", "config5")); // checks order!
	}

	@DatabaseTest
	@WithLiquibase(file = "Migrator/CreateIbisConfig.xml")
	@WithLiquibase(file = "Migrator/SetupConfigsInDatabase.xml")
	public void retrieveAllConfigNamesTestWithDB(DatabaseTestEnvironment env) throws Exception {
		TestConfiguration applicationContext = env.getConfiguration();
		ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();

		autoDiscovery.withDatabaseScanner(env.getDataSourceName());
		Map<String, Class<? extends IConfigurationClassLoader>> configs = autoDiscovery.scan(true);
		assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "config1", "config2", "config3", "config4", "config5")); // checks order!

		assertNull(configs.get("IAF_Util"));
		assertNull(configs.get("TestConfiguration"));

		assertEquals(DatabaseClassLoader.class, configs.get("configuration1"));
		assertEquals(DatabaseClassLoader.class, configs.get("configuration2"));
	}

	@Test
	public void retrieveAllConfigNamesTestWithFS() throws Exception {
		TestConfiguration applicationContext = new TestConfiguration();
		ResultSetBuilder builder = ResultSetBuilder.create()
				.setValue("configuration1")
				.addRow().setValue("configuration2")
				.addRow().setValue("configuration3")
				.addRow().setValue("configuration4")
				.addRow().setValue("configuration5");
		applicationContext.mockQuery("SELECT COUNT(*) FROM IBISCONFIG", builder.build());
		Map<String, Class<? extends IConfigurationClassLoader>> configs = ConfigurationUtils.retrieveAllConfigNames(applicationContext, true, true);

		assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "ClassLoader", "Config", "configuration1", "configuration4", "configuration2", "configuration3", "configuration5")); // checks order!

		assertNull(configs.get("IAF_Util"));
		assertNull(configs.get("TestConfiguration"));

		assertEquals(DirectoryClassLoader.class, configs.get("ClassLoader"));
		assertEquals(DirectoryClassLoader.class, configs.get("Config"));
		assertEquals(DatabaseClassLoader.class, configs.get("configuration1"));
		assertEquals(DatabaseClassLoader.class, configs.get("configuration2"));
	}

	@Test
	public void testConfigurationDirectoryAutoLoadInvalidClassName() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType("not-a-ClassLoader");
			assertEquals(DirectoryClassLoader.class, clazz);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("FATAL - invalid classloader type provided for [configurations.directory.classLoaderType] value [not-a-ClassLoader]"));
		}
	}

	@Test
	public void testConfigurationDirectoryAutoLoadIncompatibleClassType() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType(WebAppClassLoader.class.getSimpleName());
			assertEquals(DirectoryClassLoader.class, clazz);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("FATAL - incompatible classloader type provided for [configurations.directory.classLoaderType] value [WebAppClassLoader]"));
		}
	}

	@Test
	public void testConfigurationDirectoryAutoLoadDefaultClassName() {
		Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType("DirectoryClassLoader");
		assertEquals(DirectoryClassLoader.class, clazz);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ScanningDirectoryClassLoader", "org.frankframework.configuration.classloaders.ScanningDirectoryClassLoader"}) // Tests both simple and canonical names
	public void testConfigurationDirectoryAutoLoadScanningDirectoryClassName(String name) {
		Class<?> clazz = ConfigurationUtils.getDefaultDirectoryClassLoaderType(name);
		assertEquals(ScanningDirectoryClassLoader.class, clazz);
	}
}
