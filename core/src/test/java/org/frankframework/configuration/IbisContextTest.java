package org.frankframework.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.configuration.classloaders.AbstractClassLoader;
import org.frankframework.configuration.classloaders.DummyClassLoader;
import org.frankframework.lifecycle.events.MessageEventListener;
import org.frankframework.testutil.JunitTestClassLoaderWrapper;
import org.frankframework.testutil.NullClassLoader;
import org.frankframework.util.AppConstants;
import org.frankframework.util.MessageKeeperMessage;

public class IbisContextTest {

	private static final String BASE_DIR = "/ClassLoader";
	private static final String JAR_FILE = BASE_DIR+ "/zip/classLoader-test.zip";

	private AppConstants appConstants;

	public static final class IbisTestContext extends IbisContext {
		private final Map<String, Class<? extends ClassLoader>> configurations = new HashMap<>();

		public IbisTestContext() {
			configurations.clear();
		}

		public IbisTestContext(String configurationToLoad) {
			this(configurationToLoad, null);
		}

		public IbisTestContext(String configurationName, Class<? extends ClassLoader> classLoaderClass) {
			configurations.put(configurationName, classLoaderClass);
		}

		@Override
		protected Map<String, Class<? extends ClassLoader>> retrieveAllConfigNames() {
			if (configurations.isEmpty()) {
				return super.retrieveAllConfigNames();
			}
			return configurations;
		}

		@Override
		protected String @NonNull [] getSpringConfigurationFiles(@NonNull ClassLoader classLoader) {
			return new String[] { "testApplicationContext.xml" };
		}
	}

	@BeforeEach
	public void setup() {
		AppConstants.removeInstance();
		appConstants = AppConstants.getInstance();
	}

	@AfterEach
	public void tearDown() {
		AppConstants.removeInstance();
	}

	@Test
	public void startupShutdownNoConfigurations() {
		try(IbisContext context = new IbisTestContext("")) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());
		}
	}

	@Test
	public void startupDummyConfiguration() {
		try(IbisContext context = new IbisTestContext("Dummy", DummyClassLoader.class)) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());
		}
	}

	@Test
	public void unknownClassLoader() {
		String configurationName = "ConfigWithUnknownClassLoader";

		try (IbisContext context = new IbisTestContext(configurationName, ClassLoader.class)) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());

			assertEquals(1, context.getIbisManager().getConfigurations().size());
			Configuration config = context.getIbisManager().getConfiguration(configurationName);
			assertNotNull(config, "test configuration ["+configurationName+"] not found");
			assertEquals(configurationName, config.getName());

			ConfigurationException ex = config.getConfigurationException();
			assertNotNull(ex, "configuration should have an exception");
			assertThat(ex).hasMessageContaining("error instantiating ClassLoader");
			assertEquals(ClassLoaderException.class.getCanonicalName(), ex.getCause().getClass().getCanonicalName());
			Throwable[] suppressed = ex.getCause().getCause().getSuppressed();
			assertEquals(1, suppressed.length, "ClassLoaderException should have a supressed throwable with more information");
		}
	}

	@Test
	public void nullClassLoader() {
		String configurationName = "ConfigWithNullClassLoader";

		try(IbisTestContext context = new IbisTestContext(configurationName, NullClassLoader.class)) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());

			assertEquals(0, context.getIbisManager().getConfigurations().size());
			MessageEventListener events = context.getApplicationContext().getBean("MessageEventListener", MessageEventListener.class);
			MessageKeeperMessage message = events.getMessageKeeper().getMessage(events.getMessageKeeper().size()-2);
			assertNotNull(message, "unable to find MessageKeeperMessage");
			assertThat(message.getMessageText())
					.contains("error configuring ClassLoader for configuration [ConfigWithNullClassLoader]: (ClassLoaderException)")
					.contains("test-exception")
			;
		}
	}

	@Test
	public void configurationThatCannotInitialize() {
		String configurationName = "ConfigWithTestClassLoader";

		try(IbisContext context = new IbisTestContext(configurationName, NullClassLoader.class)) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());

			assertEquals(1, context.getIbisManager().getConfigurations().size());
			Configuration config = context.getIbisManager().getConfiguration(configurationName);
			assertNotNull(config, "test configuration ["+configurationName+"] not found. Found ["+context.getIbisManager().getConfigurations().getFirst().getId()+"] instead");
			assertEquals(configurationName, config.getName());

			ConfigurationException ex = config.getConfigurationException();
			assertNotNull(ex, "configuration should have an exception");
			assertThat(ex).hasMessageContaining("error instantiating configuration");
			Throwable[] suppressed = ex.getCause().getSuppressed();
			assertEquals(0, suppressed.length, "no further information");
		}
	}

	public static List<Arguments> data() {
		return Arrays.asList(
				arguments("WebAppClassLoader", "config1"),
				arguments("DirectoryClassLoader", "config2"),
				arguments("ScanningDirectoryClassLoader", "config3"),
				arguments("JarFileClassLoader", "config4")
		);
	}

	public void configureClassloader(@NonNull String type, @NonNull String configurationName, @Nullable String parentConfigurationName) {

		if (StringUtils.isNotEmpty(parentConfigurationName)) {
			setLocalProperty("configurations."+configurationName+".parentConfig", parentConfigurationName);
		}
		setLocalProperty("configurations."+configurationName+".classLoaderType", type);
		if (type.endsWith("DirectoryClassLoader")) {
			String directory = JunitTestClassLoaderWrapper.getTestClassesLocation()+"ClassLoader/DirectoryClassLoaderRoot/";
			if (StringUtils.isNotEmpty(parentConfigurationName)) {
				// Directory for child-config
				setLocalProperty("configurations."+configurationName+".directory", directory + "Config");
			} else {
				// Directory for parent-config
				setLocalProperty("configurations."+configurationName+".directory", directory + "ClassLoader");
			}
			setLocalProperty("configurations."+configurationName+".basePath", ".");
		}

		if (type.endsWith("JarFileClassLoader")) {
			URL file = this.getClass().getResource(JAR_FILE);
			assertNotNull(file, "Resource [" + JAR_FILE + "] not found");
			setLocalProperty("configurations."+configurationName+".jar", file.getFile());
		}
		setLocalProperty("configurations.names", appConstants.get("configurations.names") + "," + configurationName);
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testConfigurationsWithParentConfiguration(String classLoaderType, String configurationName) {
		String parentConfigurationName = "myParentConfig";
		configureClassloader("DirectoryClassLoader", parentConfigurationName, null);
		configureClassloader(classLoaderType, configurationName, parentConfigurationName);

		String testFile = "fileOnlyOnDirectoryClassPath.txt";

		try (IbisContext context = new IbisTestContext()) {
			context.init();
			IbisManager ibisManager = context.getIbisManager();

			Configuration configuration = ibisManager.getConfiguration(configurationName);
			Configuration parentConfiguration = ibisManager.getConfiguration(parentConfigurationName);

			assertNotNull(configuration);
			assertNotNull(parentConfiguration);

			ClassLoader parentClassLoader = parentConfiguration.getClassLoader();
			ClassLoader classLoader = configuration.getClassLoader();

			assertNotNull(classLoader);
			assertNotNull(parentClassLoader);
			assertEquals(parentClassLoader, classLoader.getParent());

			URL parentResource = parentClassLoader.getResource(testFile);
			URL resource = classLoader.getResource(testFile);

			assertNotNull(parentResource);
			assertNotNull(resource);

			if (classLoaderType.endsWith("DirectoryClassLoader")) {
    			String childOnlyTestFile = "fileOnlyOnChildDirectoryClasPath.txt";
				URL childOnlyResourceViaParent = parentClassLoader.getResource(childOnlyTestFile);
				URL childOnlyResource = classLoader.getResource(childOnlyTestFile);

				assertNull(childOnlyResourceViaParent);
				assertNotNull(childOnlyResource);
			}
		}
	}

	@ParameterizedTest
	@MethodSource("data")
	public void testReloadConfiguration(String classLoaderType, String configurationName) {
		String parentConfigurationName = "myParentConfig";
		configureClassloader("DirectoryClassLoader", parentConfigurationName, null);
		configureClassloader(classLoaderType, configurationName, parentConfigurationName);

		String testFile = "fileOnlyOnDirectoryClassPath.txt";

		try (IbisContext context = new IbisTestContext()) {
			// Arrange
			context.init();
			IbisManager ibisManager = context.getIbisManager();

			Configuration configuration = ibisManager.getConfiguration(configurationName);

			assertNotNull(configuration);
			assertFalse(configuration.isClosed());
			ClassLoader classLoader = configuration.getClassLoader();

			assertNotNull(classLoader);

			URL resource = classLoader.getResource(testFile);
			assertNotNull(resource);

			// Act
			context.reload(configurationName);

			// Assert
			Configuration configurationAfterReload = ibisManager.getConfiguration(configurationName);

			assertNotNull(configurationAfterReload);
			assertNotSame(configuration, configurationAfterReload);
			assertNotSame(classLoader, configurationAfterReload.getClassLoader());

			assertTrue(configuration.isClosed());
			assertFalse(configurationAfterReload.isClosed());

			AbstractClassLoader configurationClassLoader = assertInstanceOf(AbstractClassLoader.class, configurationAfterReload.getClassLoader());
			assertFalse(configurationClassLoader.isDestroyed());

			AbstractClassLoader originalConfigurationClassLoader = assertInstanceOf(AbstractClassLoader.class, classLoader);
			assertTrue(originalConfigurationClassLoader.isDestroyed());
		}
	}

	@Test
	public void testReloadNonExistingConfiguration() {
		String configurationName = "no-such-configuration";

		try (IbisContext context = new IbisTestContext()) {
			// Arrange
			context.init();
			IbisManager ibisManager = context.getIbisManager();

			assertNull(ibisManager.getConfiguration(configurationName));

			// Act
			context.reload(configurationName);

			// Assert
			assertNull(ibisManager.getConfiguration(configurationName));
		}
	}

	@Test
	public void testFullConfigurationReload() {
		String classLoaderType = "DirectoryClassLoader";
		String configurationName = "config5";
		String parentConfigurationName = "myParentConfig";
		configureClassloader("DirectoryClassLoader", parentConfigurationName, null);
		configureClassloader(classLoaderType, configurationName, parentConfigurationName);

		String testFile = "fileOnlyOnDirectoryClassPath.txt";

		try (IbisContext context = new IbisTestContext()) {
			// Arrange
			context.init();
			IbisManager ibisManager = context.getIbisManager();

			Configuration configuration = ibisManager.getConfiguration(configurationName);

			assertNotNull(configuration);
			assertFalse(configuration.isClosed());
			ClassLoader classLoader = configuration.getClassLoader();

			assertNotNull(classLoader);
			ClassLoader parentClassLoader = configuration.getClassLoader().getParent();
			assertNotNull(parentClassLoader);

			// Act
			context.fullReload();

			// Assert

			// Configuration no longer present in old IbisManager
			assertNull(ibisManager.getConfiguration(configurationName));

			IbisManager newIbisManager = context.getIbisManager();

			assertNotNull(newIbisManager);
			assertNotSame(ibisManager, newIbisManager);

			Configuration configurationAfterReload = newIbisManager.getConfiguration(configurationName);
			assertNotNull(configurationAfterReload);

			ClassLoader classLoaderAfterReload = configurationAfterReload.getClassLoader();
			assertNotNull(classLoaderAfterReload);

			assertNotSame(configuration, configurationAfterReload);
			assertNotSame(classLoader, classLoaderAfterReload);

			assertTrue(configuration.isClosed());
			assertFalse(configurationAfterReload.isClosed());

			URL resource = classLoaderAfterReload.getResource(testFile);
			assertNotNull(resource);

			AbstractClassLoader originalConfigurationClassLoader = assertInstanceOf(AbstractClassLoader.class, classLoader);
			AbstractClassLoader originalParentConfigurationClassLoader = assertInstanceOf(AbstractClassLoader.class, parentClassLoader);

			assertTrue(originalConfigurationClassLoader.isDestroyed());
			assertTrue(originalParentConfigurationClassLoader.isDestroyed());
		}
	}


	/**This method makes sure the property is only set in the local AppConstants instance.
	 * When you use the default put/setProperty it stores the property in the additionalProperties and
	 * upon creation of a new AppConstants instance it will set all the previously set properties
	 */
	@SuppressWarnings("deprecation")
	private void setLocalProperty(String key, String value) {
		appConstants.put(key, value);
	}
}
