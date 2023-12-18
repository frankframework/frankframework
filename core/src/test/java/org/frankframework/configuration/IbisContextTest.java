package org.frankframework.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.context.support.AbstractApplicationContext;

import org.frankframework.configuration.classloaders.DummyClassLoader;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.lifecycle.MessageEventListener;
import org.frankframework.testutil.TestClassLoader;
import org.frankframework.testutil.mock.MockIbisManager;
import org.frankframework.util.MessageKeeperMessage;

public class IbisContextTest {

	private static final class IbisTestContext extends IbisContext {
		private Map<String, Class<? extends IConfigurationClassLoader>> configurations = new HashMap<>();

		public IbisTestContext(String configurationToLoad) {
			this(configurationToLoad, null);
		}

		public IbisTestContext(String configurationName, Class<? extends IConfigurationClassLoader> classLoaderClass) {
			configurations.put(configurationName, classLoaderClass);
		}

		@Override
		protected Map<String, Class<? extends IConfigurationClassLoader>> retrieveAllConfigNames() {
			return configurations;
		}

		@Override
		protected String[] getSpringConfigurationFiles(ClassLoader classLoader) {
			List<String> springConfigurationFiles = new ArrayList<>();
			springConfigurationFiles.add("testApplicationContext.xml");

			return springConfigurationFiles.toArray(new String[springConfigurationFiles.size()]);
		}

		@Override
		protected void createApplicationContext() throws BeansException {
			super.createApplicationContext();

			IbisManager ibisManager = new MockIbisManager();
			ibisManager.setIbisContext(this);
			getApplicationContext().getBeanFactory().registerSingleton("ibisManager", ibisManager);
		}

		@Override
		public AbstractApplicationContext getApplicationContext() {
			return super.getApplicationContext();
		}
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

		try(IbisContext context = new IbisTestContext(configurationName, IConfigurationClassLoader.class)) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());

			assertEquals(1, context.getIbisManager().getConfigurations().size());
			Configuration config = context.getIbisManager().getConfiguration(configurationName);
			assertNotNull(config, "test configuration ["+configurationName+"] not found");
			assertEquals(configurationName, config.getName());

			ConfigurationException ex = config.getConfigurationException();
			assertNotNull(ex, "configuration should have an exception");
			assertThat(ex.getMessage(), Matchers.startsWith("error instantiating ClassLoader"));
			assertEquals(ClassLoaderException.class.getCanonicalName(), ex.getCause().getClass().getCanonicalName());
			Throwable[] suppressed = ex.getCause().getSuppressed();
			assertEquals(1, suppressed.length, "ClassLoaderException should have a supressed throwable with more information");
		}
	}

	@Test
	public void nullClassLoader() {
		String configurationName = "ConfigWithNullClassLoader";

		try(IbisTestContext context = new IbisTestContext(configurationName, TestClassLoader.class)) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());

			assertEquals(0, context.getIbisManager().getConfigurations().size());
			MessageEventListener events = context.getApplicationContext().getBean("MessageEventListener", MessageEventListener.class);
			MessageKeeperMessage message = events.getMessageKeeper().getMessage(events.getMessageKeeper().size()-2);
			assertNotNull(message, "unable to find MessageKeeperMessage");
			assertThat(message.getMessageText(), Matchers.endsWith("error configuring ClassLoader for configuration [ConfigWithNullClassLoader]: (ClassLoaderException) test-exception"));
		}
	}

	@Test
	public void configurationThatCannotInitialize() {
		String configurationName = "ConfigWithTestClassLoader";

		try(IbisContext context = new IbisTestContext(configurationName, TestClassLoader.class)) {
			context.init(false);

			assertEquals("TestConfiguration", context.getApplicationName());

			assertEquals(1, context.getIbisManager().getConfigurations().size());
			Configuration config = context.getIbisManager().getConfiguration(configurationName);
			assertNotNull(config, "test configuration ["+configurationName+"] not found. Found ["+context.getIbisManager().getConfigurations().get(0).getId()+"] instead");
			assertEquals(configurationName, config.getName());

			ConfigurationException ex = config.getConfigurationException();
			assertNotNull(ex, "configuration should have an exception");
			assertThat(ex.getMessage(), Matchers.startsWith("error instantiating configuration"));
			Throwable[] suppressed = ex.getCause().getSuppressed();
			assertEquals(0, suppressed.length, "no further information");
		}
	}
}
