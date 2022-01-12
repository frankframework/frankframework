/*
   Copyright 2013, 2016-2019 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

import nl.nn.adapterframework.configuration.classloaders.IConfigurationClassLoader;
import nl.nn.adapterframework.core.IScopeProvider;
import nl.nn.adapterframework.http.RestServiceDispatcher;
import nl.nn.adapterframework.jdbc.JdbcPropertySourceFactory;
import nl.nn.adapterframework.jdbc.migration.Migrator;
import nl.nn.adapterframework.lifecycle.IbisApplicationContext;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeper.MessageKeeperLevel;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

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

	private static final Logger secLog = LogUtil.getLogger("SEC");
	private static final String ALL_CONFIGS_KEY = "*ALL*";

	private final String INSTANCE_NAME = APP_CONSTANTS.getResolvedProperty("instance.name");

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

	private IbisManager ibisManager;
	private Map<String, MessageKeeper> messageKeepers = new HashMap<>();
	private int messageKeeperSize = 10;
	private FlowDiagramManager flowDiagramManager;
	private ClassLoaderManager classLoaderManager = null;
	private static List<String> loadingConfigs = new ArrayList<>();

	private Thread ibisContextReconnectThread = null;

	/**
	 * Creates the Spring context, and load the configuration. Optionally  with
	 * a specific ClassLoader which might for example override the getResource
	 * method to load configuration and related resources from a different
	 * location from the standard classpath. 
	 * 
	 * @see ClassUtils#getResourceURL(IScopeProvider, String)
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
	 * @see ClassUtils#getResourceURL(IScopeProvider, String)
	 * @see AppConstants#getInstance(ClassLoader)
	 *
	 * @param reconnect retry startup when failures occur
	 * @throws BeanCreationException when Spring can't start up
	 */
	public synchronized void init(boolean reconnect) {
		try {
			long start = System.currentTimeMillis();

			LOG.info("Attempting to start IBIS application");
			createApplicationContext();
			LOG.debug("Created Ibis Application Context");

			ibisManager = getBean("ibisManager", IbisManager.class);
			ibisManager.setIbisContext(this);
			LOG.debug("Loaded IbisManager Bean");

			MessageKeeper messageKeeper = new MessageKeeper();
			messageKeepers.put(ALL_CONFIGS_KEY, messageKeeper);

			classLoaderManager = new ClassLoaderManager(this);

			try {
				flowDiagramManager = getBean("flowDiagramManager", FlowDiagramManager.class); //The FlowDiagramManager should always initialize.
			} catch (BeanCreationException | BeanInstantiationException | NoSuchBeanDefinitionException e) {
				log(null, null, "failed to initalize FlowDiagramManager", MessageKeeperLevel.ERROR, e, true);
			}

			load();
			getMessageKeeper().setMaxSize(Math.max(messageKeeperSize, getMessageKeeper().size()));

			log("startup in " + (System.currentTimeMillis() - start) + " ms");
		}
		catch (Exception e) {
			//Catch all exceptions, the IBIS failed to startup...
			if(reconnect) {
				LOG.error("Failed to initialize IbisContext, retrying in 1 minute!", e);

				ibisContextReconnectThread = new Thread(new IbisContextRunnable(this));
				ibisContextReconnectThread.setName("IbisContext-ReconnectThread"); //Give the thread a somewhat descriptive name
				ibisContextReconnectThread.start();
			}
			else {
				LOG.error("Failed to initialize IbisContext", e);
				throw e;
			}
		}
	}

	/**
	 * Shuts down the IbisContext, and therefore the Spring context
	 * 
	 * @see #destroyApplicationContext()
	 */
	public synchronized void destroy() {
		long start = System.currentTimeMillis();

		if(ibisManager != null) {
			ibisManager.shutdown();
		}
		if(ibisContextReconnectThread != null) { //If the ibis failed to initialize, and is trying to shutdown
			ibisContextReconnectThread.interrupt();
		}
		if(classLoaderManager != null) {
			classLoaderManager.shutdown();
		}

		destroyApplicationContext();
		log("shutdown in " + (System.currentTimeMillis() - start) + " ms");
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
		} catch (ConfigurationException e) {
			log(configurationName, null, "failed to reload", MessageKeeperLevel.ERROR, e);
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
			long start = System.currentTimeMillis();
			ibisManager.unload(configurationName);
			if (configuration.getRegisteredAdapters().size() > 0) {
				log("Not all adapters are unregistered: " + configuration.getRegisteredAdapters(), MessageKeeperLevel.ERROR);
			}
			String configurationVersion = configuration.getVersion();
			getApplicationContext().getAutowireCapableBeanFactory().destroyBean(configuration);

			String msg = "unload in " + (System.currentTimeMillis() - start) + " ms";
			log(configurationName, configurationVersion, msg);
			secLog.info("Configuration [" + configurationName + "] [" + configurationVersion+"] " + msg);
		} else {
			log("Configuration [" + configurationName + "] to unload not found", MessageKeeperLevel.WARN);
		}
	}

	/**
	 * Completely rebuilds the ibisContext and therefore also the Spring context
	 * 
	 * @see #destroy()
	 * @see #init()
	 */
	public synchronized void fullReload() {
		if (isLoadingConfigs()) {
			log("Skipping fullReload because one or more configurations are currently loading", MessageKeeperLevel.WARN);
			return;
		}

		destroy();
		Set<String> javaListenerNames = JavaListener.getListenerNames();
		if (javaListenerNames.size() > 0) {
			log("Not all java listeners are unregistered: " + javaListenerNames, MessageKeeperLevel.ERROR);
		}
		Set uriPatterns = RestServiceDispatcher.getInstance().getUriPatterns();
		if (uriPatterns.size() > 0) {
			log("Not all rest listeners are unregistered: " + uriPatterns, MessageKeeperLevel.ERROR);
		}

		init();
	}

	/**
	 * Load all registered configurations
	 * @see #load(String)
	 */
	private void load() {
		if(AppConstants.getInstance().getBoolean(AppConstants.JDBC_PROPERTIES_KEY, false)) {
			JdbcPropertySourceFactory propertySourceFactory = getBean("jdbcPropertySourceFactory", JdbcPropertySourceFactory.class);
			Properties properties = propertySourceFactory.createPropertySource(getApplicationName()+"-DatabaseProperties");
			if(properties != null) {
				AppConstants.getInstance().putAll(properties);
			}
		}

		try {
			loadingConfigs.add(ALL_CONFIGS_KEY);
			load(null);
		} finally {
			loadingConfigs.remove(ALL_CONFIGS_KEY);
		}
	}

	/**
	 * Loads, digests and starts the specified configuration, or all configurations
	 * Does not check if the configuration already exists. Does not unload old configurations!
	 * 
	 * @param configurationName name of the configuration to load or null when you want to load all configurations
	 * 
	 * @see ClassLoaderManager#get(String)
	 * @see ConfigurationUtils#retrieveAllConfigNames(IbisContext)
	 * @see #createAndConfigureConfigurationWithClassLoader(ClassLoader, String, ConfigurationException)
	 */
	public void load(String configurationName) {
		boolean configFound = false;

		//We have an ordered list with all configurations, lets loop through!
		Map<String, String> allConfigNamesItems = ConfigurationUtils.retrieveAllConfigNames(this);
		for (Entry<String, String> currentConfigNameItem : allConfigNamesItems.entrySet()) {
			String currentConfigurationName = currentConfigNameItem.getKey();
			String classLoaderType = currentConfigNameItem.getValue();

			if (configurationName == null || configurationName.equals(currentConfigurationName)) {
				LOG.info("loading configuration ["+currentConfigurationName+"]");
				configFound = true;

				ConfigurationException customClassLoaderConfigurationException = null;
				ClassLoader classLoader = null;
				try {
					classLoader = classLoaderManager.get(currentConfigurationName, classLoaderType);

					//An error occurred but we don't want to throw any exceptions.
					//Skip configuration digesting so it can be done at a later time.
					if(classLoader == null)
						continue;

				} catch (ConfigurationException e) {
					customClassLoaderConfigurationException = e;
					if(LOG.isDebugEnabled()) LOG.debug("configuration ["+currentConfigurationName+"] got exception creating/retrieving classloader type ["+classLoaderType+"] errorMessage ["+e.getMessage()+"]");
				}

				if(LOG.isDebugEnabled()) LOG.debug("configuration ["+currentConfigurationName+"] found classloader ["+ClassUtils.nameOf(classLoader)+"]");
				try {
					loadingConfigs.add(currentConfigurationName);
					createAndConfigureConfigurationWithClassLoader(classLoader, currentConfigurationName, customClassLoaderConfigurationException);
				} catch (Exception e) {
					log(currentConfigurationName, null, "an unhandled exception occurred while loading configuration ["+currentConfigurationName+"]", MessageKeeperLevel.ERROR, e);
				} finally {
					loadingConfigs.remove(currentConfigurationName);
				}

				LOG.info("configuration ["+currentConfigurationName+"] loaded successfully");
			}
		}

		generateFlow();
		//Check if the configuration we try to reload actually exists
		if (!configFound && configurationName != null) {
			log(configurationName + " not found in ["+allConfigNamesItems.keySet().toString()+"]", MessageKeeperLevel.ERROR);
		}
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
		return (Configuration) getApplicationContext().getAutowireCapableBeanFactory().initializeBean(bean, name);
	}

	/**
	 * either ClassLoader is populated or ConfigurationException, but never both!
	 */
	private void createAndConfigureConfigurationWithClassLoader(ClassLoader classLoader, String currentConfigurationName, ConfigurationException classLoaderException) {

		long start = System.currentTimeMillis();
		if(LOG.isDebugEnabled()) LOG.debug("creating new configuration ["+currentConfigurationName+"]");

		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
		}

		String currentConfigurationVersion = null;
		Configuration configuration = null;
		try {
			try {
				configuration = createConfiguration(currentConfigurationName, classLoader);
			} catch (Exception e) { //May throw Spring Bean instantiation exceptions
				configuration = new Configuration(); //Instantiate a placeholder to store the exception in.
				configuration.setName(currentConfigurationName);
				ibisManager.addConfiguration(configuration);
				if(classLoaderException != null) {
					classLoaderException.addSuppressed(e);
					throw classLoaderException;
				}
				throw new ConfigurationException("error instantiating configuration", e);
			}

			if(!configuration.getName().equals(currentConfigurationName)) { //Pre-digest validation to make sure no extra Spring magic happened.
				throw new ConfigurationException("configurationName mismatch");
			}

			currentConfigurationVersion = configuration.getVersion();

			// Execute any database changes before calling configure.
			// For now explicitly call configure, fix this once ConfigurationDigester implements ConfigurableLifecycle
			if(AppConstants.getInstance(configuration.getClassLoader()).getBoolean("jdbc.migrator.active", false)) {
				try(Migrator databaseMigrator = configuration.getBean("jdbcMigrator", Migrator.class)) {
					databaseMigrator.setIbisContext(this);
					databaseMigrator.configure();
					databaseMigrator.update();
				} catch (Exception e) {
					log(currentConfigurationName, currentConfigurationVersion, "unable to run JDBC migration", MessageKeeperLevel.ERROR, e);
				}
			}

			configuration.configure();

			if (currentConfigurationVersion == null) {
				currentConfigurationVersion = configuration.getVersion(); //Digested configuration version
			} else if (!currentConfigurationVersion.equals(configuration.getVersion())) {
				log(currentConfigurationName, currentConfigurationVersion, "configuration version doesn't match Configuration version attribute: " + configuration.getVersion(), MessageKeeperLevel.WARN);
			}
			if (!currentConfigurationName.equals(configuration.getName())) {
				log(currentConfigurationName, currentConfigurationVersion, "configuration name doesn't match Configuration name attribute: " + configuration.getName(), MessageKeeperLevel.WARN);
				messageKeepers.put(configuration.getName(), messageKeepers.remove(currentConfigurationName));
			}

			String msg;
			if (configuration.isAutoStart()) {
				ibisManager.startConfiguration(configuration);
				msg = "startup in " + (System.currentTimeMillis() - start) + " ms";
			}
			else {
				msg = "configured in " + (System.currentTimeMillis() - start) + " ms";
			}

			log(currentConfigurationName, currentConfigurationVersion, msg);
			secLog.info("Configuration [" + currentConfigurationName + "] [" + currentConfigurationVersion+"] " + msg);

			LOG.info("configured configuration ["+currentConfigurationName+"] successfully");
		} catch (ConfigurationException e) {
			if(configuration != null) {
				configuration.setConfigurationException(e);
			}
			log(currentConfigurationName, currentConfigurationVersion, "exception", MessageKeeperLevel.ERROR, e);
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	private void generateFlow() { //Generate big flow diagram file for all configurations
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
		log(null, null, message, MessageKeeperLevel.INFO, null, true);
	}

	private void log(String message, MessageKeeperLevel level) {
		log(null, null, message, level, null, true);
	}

	public void log(String message, MessageKeeperLevel level, Exception e) {
		log(null, null, message, level, e, true);
	}

	public void log(String configurationName, String configurationVersion, String message) {
		log(configurationName, configurationVersion, message, MessageKeeperLevel.INFO);
	}

	private void log(String configurationName, String configurationVersion, String message, MessageKeeperLevel level) {
		log(configurationName, configurationVersion, message, level, null, false);
	}

	public void log(String configurationName, String configurationVersion, String message, MessageKeeperLevel level, Exception e) {
		log(configurationName, configurationVersion, message, level, e, false);
	}

	private void log(String configurationName, String configurationVersion, String message, MessageKeeperLevel level, Exception e, boolean allOnly) {
		String key;
		if (allOnly || configurationName == null) {
			key = ALL_CONFIGS_KEY;
		} else {
			key = configurationName;
		}
		MessageKeeper messageKeeper = messageKeepers.get(key);
		if (messageKeeper == null) {
			messageKeeper = new MessageKeeper(messageKeeperSize < 1 ? 1 : messageKeeperSize);
			messageKeepers.put(key, messageKeeper);
		}
		String m;
		String version;
		if (configurationName != null) {
			m = "Configuration [" + configurationName + "] ";
			version = configurationVersion;
		} else {
			m = "Application [" + INSTANCE_NAME + "] ";
			version = getApplicationVersion();
		}
		if (version != null) {
			m = m + "[" + version + "] ";
		}
		m = m + message;
		if (MessageKeeperLevel.ERROR.equals(level)) {
			LOG.error(m, e);
		} else if (MessageKeeperLevel.WARN.equals(level)) {
			LOG.warn(m, e);
		} else {
			LOG.info(m, e);
		}
		if (e != null) {
			m = m + ": " + e.getMessage();
		}
		messageKeeper.add(m, level);
		if (!allOnly) {
			log(configurationName, configurationVersion, message, level, e, true);
		}
	}

	/**
	 * Get MessageKeeper for a specific configuration. The MessageKeeper is not
	 * stored at the Configuration object instance to prevent messages being
	 * lost after configuration reload.
	 * @return MessageKeeper for '*ALL*' configurations
	 */
	public MessageKeeper getMessageKeeper() {
		return getMessageKeeper(ALL_CONFIGS_KEY);
	}

	/**
	 * Get MessageKeeper for a specific configuration. The MessageKeeper is not
	 * stored at the Configuration object instance to prevent messages being
	 * lost after configuration reload.
	 * @param configurationName configuration name to get the MessageKeeper object from
	 * @return MessageKeeper for specified configurations
	 */
	public MessageKeeper getMessageKeeper(String configurationName) {
		return messageKeepers.get(configurationName);
	}

	public IbisManager getIbisManager() {
		return ibisManager;
	}

	public String getApplicationName() {
		return APP_CONSTANTS.getProperty("instance.name", null);
	}

	private String getApplicationVersion() {
		return ConfigurationUtils.getApplicationVersion();
	}

	public boolean isLoadingConfigs() {
		return !loadingConfigs.isEmpty();
	}
}
