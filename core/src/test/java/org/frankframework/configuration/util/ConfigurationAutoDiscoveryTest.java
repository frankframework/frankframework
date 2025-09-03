package org.frankframework.configuration.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ClassLoaderException;
import org.frankframework.configuration.IbisContext;
import org.frankframework.configuration.classloaders.DatabaseClassLoader;
import org.frankframework.configuration.classloaders.DirectoryClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.configuration.classloaders.JarFileClassLoader;
import org.frankframework.configuration.classloaders.ScanningDirectoryClassLoader;
import org.frankframework.configuration.classloaders.WebAppClassLoader;
import org.frankframework.configuration.util.ConfigurationAutoDiscovery.ParentConfigComparator;
import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.testutil.TestAppender;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.junit.DatabaseTest;
import org.frankframework.testutil.junit.DatabaseTestEnvironment;
import org.frankframework.testutil.junit.WithLiquibase;
import org.frankframework.util.AppConstants;

public class ConfigurationAutoDiscoveryTest {

	@BeforeAll
	public static void setUp() throws Exception {
		AppConstants.getInstance().setProperty("configurations.configuration2.parentConfig", "configuration4");
		AppConstants.getInstance().setProperty("configurations.configuration3.parentConfig", "configuration4");
		AppConstants.getInstance().setProperty("configurations.Config.parentConfig", "ClassLoader");

		URL url = TestFileUtils.getTestFileURL("/ClassLoader/DirectoryClassLoaderRoot");
		assertNotNull(url);
		File directory = new File(url.toURI());
		AppConstants.getInstance().setProperty("configurations.directory", directory.getCanonicalPath());
	}

	@AfterAll
	@BeforeAll
	public static void removeAppConstantsInstance() {
		AppConstants.removeInstance();
	}

	@DatabaseTest
	@WithLiquibase(file = "Migrator/CreateIbisConfig.xml")
	@WithLiquibase(file = "Migrator/SetupConfigsInDatabase.xml")
	public void retrieveConfigNamesFromDatabaseTest(DatabaseTestEnvironment env) {
		TestConfiguration applicationContext = env.getConfiguration();
		ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();
		autoDiscovery.withDatabaseScanner(env.getDataSourceName());

		Set<String> configs = autoDiscovery.scan(false).keySet();

		assertThat(configs, IsIterableContainingInOrder.contains("config1", "config2", "config3", "config4", "config5"));
	}

	@DatabaseTest
	@WithLiquibase(file = "Migrator/CreateIbisConfig.xml")
	@WithLiquibase(file = "Migrator/SetupConfigsInDatabase.xml")
	public void retrieveAllConfigNamesTestWithDB(DatabaseTestEnvironment env) {
		TestConfiguration applicationContext = env.getConfiguration();
		ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();
		autoDiscovery.withDatabaseScanner(env.getDataSourceName());

		Map<String, Class<? extends IConfigurationClassLoader>> configs = autoDiscovery.scan(true);
		assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "config1", "config2", "config3", "config4", "config5"));

		assertNull(configs.get("IAF_Util"));
		assertNull(configs.get("TestConfiguration"));

		assertEquals(DatabaseClassLoader.class, configs.get("config1"));
		assertEquals(DatabaseClassLoader.class, configs.get("config2"));
	}

	@DatabaseTest // Note, there is no liquibase here ;)
	public void retrieveAllConfigNamesTestWithoutDB(DatabaseTestEnvironment env) {
		TestConfiguration applicationContext = env.getConfiguration();
		ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();
		autoDiscovery.withDatabaseScanner(env.getDataSourceName());

		try (TestAppender appender = TestAppender.newBuilder().build()) {
			Map<String, Class<? extends IConfigurationClassLoader>> configs = autoDiscovery.scan(true);
			assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration"));

			assertNull(configs.get("IAF_Util"));
			assertNull(configs.get("TestConfiguration"));

			assertTrue(appender.contains("unable to load configurations from database, table [IBISCONFIG] is not present"));
		}
	}

	@DatabaseTest
	@WithLiquibase(file = "Migrator/CreateIbisConfig.xml")
	@WithLiquibase(file = "Migrator/SetupConfigsInDatabase.xml")
	public void retrieveAllConfigNamesTestWithDbAndFs(DatabaseTestEnvironment env) throws IOException {
		TestConfiguration applicationContext = env.getConfiguration();
		ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();
		autoDiscovery.withDatabaseScanner(env.getDataSourceName());
		autoDiscovery.withDirectoryScanner();

		Map<String, Class<? extends IConfigurationClassLoader>> configs = autoDiscovery.scan(true);

		assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "ClassLoader", "Config", "JarConfig1", "config1", "config2", "config3", "config4", "config5"));

		assertNull(configs.get("IAF_Util"));
		assertNull(configs.get("TestConfiguration"));

		assertEquals(DirectoryClassLoader.class, configs.get("ClassLoader"));
		assertEquals(DirectoryClassLoader.class, configs.get("Config"));
		assertEquals(JarFileClassLoader.class, configs.get("JarConfig1"));
		assertEquals(DatabaseClassLoader.class, configs.get("config1"));
		assertEquals(DatabaseClassLoader.class, configs.get("config2"));
	}

	@Test
	public void retrieveAllConfigNamesTestWithFS() throws IOException {
		try (TestConfiguration applicationContext = new TestConfiguration()) {
			ConfigurationAutoDiscovery autoDiscovery = applicationContext.createBean();
			autoDiscovery.withDirectoryScanner();

			Map<String, Class<? extends IConfigurationClassLoader>> configs = autoDiscovery.scan(true);

			assertThat("keyset was: " + configs.keySet(), configs.keySet(), IsIterableContainingInOrder.contains("IAF_Util", "TestConfiguration", "ClassLoader", "Config", "JarConfig1"));

			assertNull(configs.get("IAF_Util"));
			assertNull(configs.get("TestConfiguration"));

			assertEquals(DirectoryClassLoader.class, configs.get("ClassLoader"));
			assertEquals(DirectoryClassLoader.class, configs.get("Config"));
			assertEquals(JarFileClassLoader.class, configs.get("JarConfig1"));
		}
	}

	@Test
	public void findConfiguration() throws ClassLoaderException {
		ClassLoader classLoader = new JunitTestClassLoaderWrapper(); // Add ability to retrieve classes from src/test/resources
		JarFileClassLoader cl = new JarFileClassLoader(classLoader);
		IbisContext ibisContext = mock(IbisContext.class);
		cl.configure(ibisContext, "JarConfig1");
		assertNotNull(cl.getResource("Configuration.xml"));
	}

	@Test
	public void testClassloaderOrderComparator() {
		List<String> configs = new ArrayList<>(List.of("configuration1", "configuration2", "configuration3", "configuration4", "configuration5"));
		ParentConfigComparator comparator = new ParentConfigComparator();
		configs.sort(comparator);
		assertThat("keyset was: " + configs, configs, IsIterableContainingInOrder.contains("configuration1", "configuration4", "configuration2", "configuration3", "configuration5")); // checks order!
	}

	@Test
	public void testClassloaderOrder() {
		Map<String, Class<? extends IConfigurationClassLoader>> configs = new LinkedHashMap<>();
		configs.put("configuration2", DirectoryClassLoader.class);
		configs.put("configuration4", DirectoryClassLoader.class);
		configs.put("configuration3", DirectoryClassLoader.class);
		configs.put("configuration1", DirectoryClassLoader.class);
		configs.put("configuration5", DirectoryClassLoader.class);

		Map<String, Class<? extends IConfigurationClassLoader>> sorted = ConfigurationAutoDiscovery.sort(configs);

		assertThat("keyset was: " + sorted.keySet(), sorted.keySet(), IsIterableContainingInOrder.contains("configuration4", "configuration2", "configuration3", "configuration1", "configuration5")); // checks order!
	}

	@Test
	public void testConfigurationDirectoryAutoLoadInvalidClassName() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			Class<?> clazz = ConfigurationAutoDiscovery.getDefaultDirectoryClassLoaderType("not-a-ClassLoader");
			assertEquals(DirectoryClassLoader.class, clazz);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("FATAL - invalid classloader type provided for [configurations.directory.classLoaderType] value [not-a-ClassLoader]"));
		}
	}

	@Test
	public void testConfigurationDirectoryAutoLoadIncompatibleClassType() {
		try (TestAppender appender = TestAppender.newBuilder().useIbisPatternLayout("%level - %m").build()) {
			Class<?> clazz = ConfigurationAutoDiscovery.getDefaultDirectoryClassLoaderType(WebAppClassLoader.class.getSimpleName());
			assertEquals(DirectoryClassLoader.class, clazz);

			// Normally adapter is present in the ThreadContext, but this is not set by the pipe processors
			assertTrue(appender.contains("FATAL - incompatible classloader type provided for [configurations.directory.classLoaderType] value [WebAppClassLoader]"));
		}
	}

	@Test
	public void testConfigurationDirectoryAutoLoadDefaultClassName() {
		Class<?> clazz = ConfigurationAutoDiscovery.getDefaultDirectoryClassLoaderType("DirectoryClassLoader");
		assertEquals(DirectoryClassLoader.class, clazz);
	}

	@ParameterizedTest
	@ValueSource(strings = {"ScanningDirectoryClassLoader", "org.frankframework.configuration.classloaders.ScanningDirectoryClassLoader"}) // Tests both simple and canonical names
	public void testConfigurationDirectoryAutoLoadScanningDirectoryClassName(String name) {
		Class<?> clazz = ConfigurationAutoDiscovery.getDefaultDirectoryClassLoaderType(name);
		assertEquals(ScanningDirectoryClassLoader.class, clazz);
	}
}
