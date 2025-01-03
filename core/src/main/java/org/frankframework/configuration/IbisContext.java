/*
   Copyright 2013, 2016-2019 Nationale-Nederlanden, 2020-2024 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.bus.BusMessageUtils;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import org.frankframework.configuration.classloaders.IConfigurationClassLoader;
import org.frankframework.core.IScopeProvider;
import org.frankframework.http.RestServiceDispatcher;
import org.frankframework.jdbc.JdbcPropertySourceFactory;
import org.frankframework.lifecycle.ApplicationMessageEvent;
import org.frankframework.lifecycle.IbisApplicationContext;
import org.frankframework.receivers.JavaListener;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.MessageKeeper.MessageKeeperLevel;
import org.frankframework.util.flow.FlowDiagramManager;

/**
 * Main entry point for creating and starting Ibis instances from
 * the configuration file.
 *
 * This class can not be created from the Spring context, because it
 * is the place where the Spring context is created.
 *
 *
 *
 * @author Tim van der Leeuw
 * @author Jaco de Groot
 * @since 4.8
 */
public class IbisContext extends IbisApplicationContext {
	private static final Logger LOG = LogUtil.getLogger(IbisContext.class);
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	static {
		if(!Boolean.parseBoolean(APP_CONSTANTS.getProperty("jdbc.convertFieldnamesToUppercase")))
			ApplicationWarnings.add(LOG, "DEPRECATED: jdbc.convertFieldnamesToUppercase is set to false, please set to true. XML field definitions of SQL senders will be uppercased!");

		String loadFileSuffix = APP_CONSTANTS.getProperty(AppConstants.ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY);
		if (StringUtils.isNotEmpty(loadFileSuffix))
			ApplicationWarnings.add(LOG, "DEPRECATED: SUFFIX [_"+loadFileSuffix+"] files are deprecated, property files are now inherited from their parent!");

		String autoDatabaseClassLoader = APP_CONSTANTS.getProperty("configurations.autoDatabaseClassLoader");
		if (StringUtils.isNotEmpty(autoDatabaseClassLoader))
			ApplicationWarnings.add(LOG, "DEPRECATED property [configurations.autoDatabaseClassLoader], please use [configurations.database.autoLoad] instead");
	}

	private @Getter IbisManager ibisManager;
	private FlowDiagramManager flowDiagramManager;
	private ClassLoaderManager classLoaderManager = null;
	private static final List<String> loadingConfigs = new ArrayList<>();

	private Thread ibisContextReconnectThread = null;

	/**
	 * Creates the Spring context, and load the configuration. Optionally  with
	 * a specific ClassLoader which might for example override the getResource
	 * method to load configuration and related resources from a different
	 * location from the standard classpath.
	 *
	 * @see ClassLoaderUtils#getResourceURL(IScopeProvider, String)
	 * @see AppConstants#getInstance(ClassLoader)
	 */
	public void init() {
		init(true);
	}

	/**
	 * Creates the Spring context, and load the configuration. Optionally  with
	 * a specific ClassLoader which might for example override the getResource
	 * method to load configuration and related resources from a different
	 * location from the standard classpath.
	 *
	 * @see ClassLoaderUtils#getResourceURL(IScopeProvider, String)
	 * @see AppConstants#getInstance(ClassLoader)
	 *
	 * @param reconnect retry startup when failures occur
	 * @throws BeanCreationException when Spring can't start up
	 */
	public synchronized void init(boolean reconnect) {
		try {
			long start = System.currentTimeMillis();

			APPLICATION_LOG.debug("Starting application [{}]", this::getApplicationName);
			createApplicationContext();
			LOG.debug("Created Ibis Application Context");

			ibisManager = getBean("ibisManager", IbisManager.class);
			ibisManager.setIbisContext(this);
			LOG.debug("Loaded IbisManager Bean");

			classLoaderManager = new ClassLoaderManager(this);

			try {
				flowDiagramManager = getBean("flowDiagramManager", FlowDiagramManager.class); //The FlowDiagramManager should always initialize.
			} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
				log("failed to initalize FlowDiagramManager", MessageKeeperLevel.ERROR, e);
			}

			load();

			long startupTime = System.currentTimeMillis() - start;
			log("startup in " + startupTime + " ms");
			APPLICATION_LOG.info("Application [{}] startup in {} ms", this::getApplicationName, ()-> startupTime);
		}
		catch (Exception e) {
			// Catch all exceptions, the IBIS failed to startup...
			if(reconnect) {
				APPLICATION_LOG.error("Failed to initialize IbisContext, retrying in 1 minute!", e);

				ibisContextReconnectThread = new Thread(new IbisContextRunnable(this));
				ibisContextReconnectThread.setName("IbisContext-ReconnectThread"); // Give the thread a somewhat descriptive name
				ibisContextReconnectThread.start();
			}
			else {
				APPLICATION_LOG.fatal("Failed to initialize IbisContext", e);
				throw e;
			}
		}
	}

	/**
	 * Shuts down the IbisContext, and therefore the Spring context
	 *
	 * @see IbisApplicationContext#close()
	 */
	@Override
	public synchronized void close() {
		LOG.debug("Shutting down application [{}]", this::getApplicationName);
		long start = System.currentTimeMillis();

		if(ibisManager != null) {
			ibisManager.shutdown();
		}
		if(ibisContextReconnectThread != null) { // If the ibis failed to initialize, and is trying to shutdown
			ibisContextReconnectThread.interrupt();
		}
		if(classLoaderManager != null) {
			classLoaderManager.shutdown();
		}

		long shutdownTime = System.currentTimeMillis() - start;
		log("shutdown in " + shutdownTime + " ms"); // Should log this before the actual Context is destroyed
		super.close();
		APPLICATION_LOG.info("Application [{}] shutdown in {} ms", this::getApplicationName, ()-> shutdownTime);
	}

	/**
	 * Reloads the given configuration. First it checks if the resources can be found.
	 * It then replaces the old resources with the new resources. If all is successful
	 * it will unload the old configuration from the IbisManager, and load the new
	 * configuration.
	 */
	public synchronized void reload(String configurationName) {
		try {
			classLoaderManager.reload(configurationName);
			unload(configurationName);
			load(configurationName);
		} catch (ClassLoaderException e) {
			log("failed to reload", MessageKeeperLevel.ERROR, e);
		}
	}

	/**
	 * Be aware that the configuration may be unloaded but it's resources wont!
	 * There is currently no way to cleanup old ClassLoaders, these are kept in memory.
	 * Removing the ClassLoader will cause a ClassLoader-leak, leaving a small footprint behind in memory!
	 * Use {@link IbisContext#reload(String)} where possible.
	 */
	public void unload(String configurationName) {
		Configuration configuration = ibisManager.getConfiguration(configurationName);
		if (configuration != null) {
			ibisManager.unload(configurationName);
			if (!configuration.getRegisteredAdapters().isEmpty()) {
				log("Not all adapters are unregistered: " + configuration.getRegisteredAdapters(), MessageKeeperLevel.ERROR);
			}
			getApplicationContext().getAutowireCapableBeanFactory().destroyBean(configuration);
		} else {
			log("Configuration [" + configurationName + "] to unload not found", MessageKeeperLevel.WARN);
		}
	}

	/**
	 * Completely rebuilds the ibisContext and therefore also the Spring context
	 *
	 * @see #close()
	 * @see #init()
	 */
	public synchronized void fullReload() {
		if (isLoadingConfigs()) {
			log("Skipping fullReload because one or more configurations are currently loading", MessageKeeperLevel.WARN);
			return;
		}

		close();
		Set<String> javaListenerNames = JavaListener.getListenerNames();
		if (!javaListenerNames.isEmpty()) {
			// cannot log to MessageKeeper here, as applicationContext is closed
			LOG.warn("Not all java listeners are unregistered: {}", javaListenerNames);
		}
		Set<String> uriPatterns = RestServiceDispatcher.getInstance().getUriPatterns();
		if (!uriPatterns.isEmpty()) {
			// cannot log to MessageKeeper here, as applicationContext is closed
			LOG.warn("Not all rest listeners are unregistered: {}", uriPatterns);
		}

		init();
	}

	/**
	 * Load all registered configurations
	 * @see #load(String)
	 */
	private void load() {
		if(AppConstants.getInstance().getBoolean(JdbcPropertySourceFactory.JDBC_PROPERTIES_KEY, false)) {
			JdbcPropertySourceFactory propertySourceFactory = getBean("jdbcPropertySourceFactory", JdbcPropertySourceFactory.class);
			Properties properties = propertySourceFactory.createPropertySource(getApplicationName()+"-DatabaseProperties");
			if(properties != null) {
				AppConstants.getInstance().putAll(properties);
			}
		}

		try {
			loadingConfigs.add(BusMessageUtils.ALL_CONFIGS_KEY);
			load(null);
		} finally {
			loadingConfigs.remove(BusMessageUtils.ALL_CONFIGS_KEY);
		}
	}

	/**
	 * Loads, digests and starts the specified configuration, or all configurations
	 * Does not check if the configuration already exists. Does not unload old configurations!
	 *
	 * @param configurationName name of the configuration to load or null when you want to load all configurations
	 *
	 * @see ClassLoaderManager#get(String)
	 * @see ConfigurationUtils#retrieveAllConfigNames(ApplicationContext)
	 * @see #createAndConfigureConfigurationWithClassLoader(ClassLoader, String, ClassLoaderException)
	 */
	public void load(String configurationName) {
		boolean configFound = false;

		// We have an ordered list with all configurations, lets loop through!
		Map<String, Class<? extends IConfigurationClassLoader>> allConfigNamesItems = retrieveAllConfigNames();
		for (Entry<String, Class<? extends IConfigurationClassLoader>> currentConfigNameItem : allConfigNamesItems.entrySet()) {
			String currentConfigurationName = currentConfigNameItem.getKey();
			String classLoaderType = currentConfigNameItem.getValue() == null ? null : currentConfigNameItem.getValue().getCanonicalName();

			if (configurationName == null || configurationName.equals(currentConfigurationName)) {
				LOG.info("loading configuration [{}]", currentConfigurationName);
				configFound = true;

				ClassLoaderException classLoaderException = null;
				ClassLoader classLoader = null;
				try {
					classLoader = classLoaderManager.get(currentConfigurationName, classLoaderType);

					// An error occurred but we don't want to throw any exceptions.
					// Skip configuration digesting so it can be done at a later time.
					if(classLoader == null)
						continue;

				} catch (ClassLoaderException e) {
					classLoaderException = e;
					if(LOG.isDebugEnabled())
						LOG.debug("configuration [{}] got exception creating/retrieving classloader type [{}] errorMessage [{}]", currentConfigurationName, classLoaderType, e.getMessage());
				}

				if(LOG.isDebugEnabled()) LOG.debug("configuration [{}] found classloader [{}]", currentConfigurationName, ClassUtils.nameOf(classLoader));
				try {
					loadingConfigs.add(currentConfigurationName);
					createAndConfigureConfigurationWithClassLoader(classLoader, currentConfigurationName, classLoaderException);
				} catch (Exception e) {
					log("an exception occurred while loading configuration ["+currentConfigurationName+"]", MessageKeeperLevel.ERROR, e);
				} finally {
					loadingConfigs.remove(currentConfigurationName);
				}

				LOG.info("configuration [{}] loaded successfully", currentConfigurationName);
			}
		}

		generateFlow();
		// Check if the configuration we try to reload actually exists
		if (!configFound && configurationName != null) {
			log(configurationName + " not found in ["+allConfigNamesItems.keySet().toString()+"]", MessageKeeperLevel.ERROR);
		}
	}

	/** Helper method to create stubbed configurations used in JunitTests */
	protected Map<String, Class<? extends IConfigurationClassLoader>> retrieveAllConfigNames() {
		return ConfigurationUtils.retrieveAllConfigNames(getApplicationContext());
	}

	/**
	 * Create a new configuration through Spring, and explicitly set the ClassLoader before initializing it.
	 * If no ClassLoader or ClassLoader is not IConfigurationClassLoader return an error.
	 */
	private Configuration createConfiguration(String name, ClassLoader classLoader) {
		if(!(classLoader instanceof IConfigurationClassLoader)) {
			throw new IllegalStateException("no IConfigurationClassLoader set");
		}

		Configuration bean = (Configuration) getApplicationContext().getAutowireCapableBeanFactory().autowire(Configuration.class, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		bean.setClassLoader(classLoader);
		Configuration configuration = (Configuration) getApplicationContext().getAutowireCapableBeanFactory().initializeBean(bean, name);

		// Pre-digest validation to make sure no extra Spring magic happened.
		if(!configuration.getName().equals(name)) {
			throw new IllegalStateException("configuration name mismatch");
		}
		return configuration;
	}

	/**
	 * either ClassLoader is populated or ConfigurationException, but never both!
	 */
	private void createAndConfigureConfigurationWithClassLoader(ClassLoader classLoader, String currentConfigurationName, ClassLoaderException classLoaderException) {
		if(LOG.isDebugEnabled()) LOG.debug("creating new configuration [{}]", currentConfigurationName);

		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}

		Configuration configuration = null;
		try {
			try { // May throw Spring Bean instantiation exceptions
				configuration = createConfiguration(currentConfigurationName, classLoader);
			} catch (Exception e) { // Instantiate a placeholder to store the exception in.
				configuration = new Configuration();
				configuration.setName(currentConfigurationName);
				ibisManager.addConfiguration(configuration); // Manually add the configuration else it will be GC'd
				if(classLoaderException != null) {
					classLoaderException.addSuppressed(e);
					throw new ConfigurationException("error instantiating ClassLoader", classLoaderException);
				}
				throw new ConfigurationException("error instantiating configuration", e);
			}

			configuration.configure();

			LOG.info("configured configuration [{}] successfully", currentConfigurationName);
		} catch (ConfigurationException e) {
			configuration.setConfigurationException(e);
			log("exception loading configuration ["+currentConfigurationName+"]", MessageKeeperLevel.ERROR, e);
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void generateFlow() { // Generate big flow diagram file for all configurations
		if (flowDiagramManager != null) {
			List<Configuration> configurations = ibisManager.getConfigurations();
			try {
				flowDiagramManager.generate(configurations);
			} catch (IOException e) {
				log("error generating flow diagram", MessageKeeperLevel.WARN, e);
			}
		}
	}

	private void log(String message) {
		log(message, MessageKeeperLevel.INFO);
	}

	private void log(String message, MessageKeeperLevel level) {
		log(message, level, null);
	}

	public void log(String message, MessageKeeperLevel level, Exception e) {
		getApplicationContext().publishEvent(new ApplicationMessageEvent(getApplicationContext(), message, level, e));
	}

	public String getApplicationName() {
		return APP_CONSTANTS.getProperty("instance.name", null);
	}

	public boolean isLoadingConfigs() {
		return !loadingConfigs.isEmpty();
	}
}
