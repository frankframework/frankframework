/*
   Copyright 2013, 2016-2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeanInstantiationException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import nl.nn.adapterframework.http.RestServiceDispatcher;
import nl.nn.adapterframework.jdbc.migration.Migrator;
import nl.nn.adapterframework.lifecycle.IbisApplicationContext;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.JdbcUtil;
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
		if(!Boolean.parseBoolean(AppConstants.getInstance().getProperty("jdbc.convertFieldnamesToUppercase")))
			ConfigurationWarnings.add(LOG, "DEPRECATED: jdbc.convertFieldnamesToUppercase is set to false, please set to true. XML field definitions of SQL senders will be uppercased!");

		String loadFileSuffix = AppConstants.getInstance().getProperty("ADDITIONAL.PROPERTIES.FILE.SUFFIX");
		if (StringUtils.isNotEmpty(loadFileSuffix))
			ConfigurationWarnings.add(LOG, "DEPRECATED: SUFFIX [_"+loadFileSuffix+"] files are deprecated, property files are now inherited from their parent!");
	}

	private IbisManager ibisManager;
	private Map<String, MessageKeeper> messageKeepers = new HashMap<>();
	private int messageKeeperSize = 10;
	private FlowDiagramManager flowDiagramManager;
	private ClassLoaderManager classLoaderManager = null;
	private static List<String> loadingConfigs = new ArrayList<>();

	/**
	 * Creates the Spring context, and load the configuration. Optionally  with
	 * a specific ClassLoader which might for example override the getResource
	 * method to load configuration and related resources from a different
	 * location from the standard classpath. 
	 * 
	 * @see ClassUtils#getResourceURL(ClassLoader, String)
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
	 * @see ClassUtils#getResourceURL(ClassLoader, String)
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

			AbstractSpringPoweredDigesterFactory.setIbisContext(this);

			try {
				flowDiagramManager = getBean("flowDiagramManager", FlowDiagramManager.class); //The FlowDiagramManager should always initialise.
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
				ibisContextReconnectThread.setName("ibisContextReconnectThread");
				ibisContextReconnectThread.start();
			}
			else {
				LOG.error("Failed to initialize IbisContext", e);
				throw e;
			}
		}
	}

	Thread ibisContextReconnectThread = null;

	/**
	 * Shuts down the IbisContext, and therefore the Spring context
	 * 
	 * @see #destroyApplicationContext()
	 */
	public synchronized void destroy() {
		long start = System.currentTimeMillis();
		if(ibisManager != null)
			ibisManager.shutdown();
		if(ibisContextReconnectThread != null)
			ibisContextReconnectThread.interrupt();
		if(classLoaderManager != null)
			classLoaderManager.shutdown();

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
			if (configuration.getAdapterService().getAdapters().size() > 0) {
				log("Not all adapters are unregistered: " + configuration.getAdapterService().getAdapters(), MessageKeeperLevel.ERROR);
			}
			// Improve configuration reload performance. Probably because
			// garbage collection will be easier.
			configuration.setAdapterService(null);
			String configurationVersion = configuration.getVersion();
			String msg = "unload in " + (System.currentTimeMillis() - start) + " ms";
			log(configurationName, configurationVersion, msg);
			secLog.info("Configuration [" + configurationName + "] [" + configurationVersion+"] " + msg);
		} else {
			log("Configuration [" + configurationName + "] to unload not found", MessageKeeperLevel.WARN);
		}
		JdbcUtil.resetJdbcProperties();
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
		JdbcUtil.resetJdbcProperties();

		init();
	}

	/**
	 * Load all registered configurations
	 * @see #load(String)
	 */
	private void load() {
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
	 * @see #digestClassLoaderConfiguration(ClassLoader, ConfigurationDigester, String, ConfigurationException)
	 */
	public void load(String configurationName) {
		boolean configFound = false;

		//We have an ordered list with all configurations, lets loop through!
		ConfigurationDigester configurationDigester = new ConfigurationDigester();

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
					digestClassLoaderConfiguration(classLoader, configurationDigester, currentConfigurationName, customClassLoaderConfigurationException);
				} finally {
					loadingConfigs.remove(currentConfigurationName);
				}

				LOG.info("configuration ["+currentConfigurationName+"] loaded successfully");
			}
		}

		generateFlow();
		//Check if the configuration we try to reload actually exists
		if (!configFound && configurationName != null) {
			log(configurationName, null, configurationName + " not found in ["+allConfigNamesItems.keySet().toString()+"]", MessageKeeperLevel.ERROR);
		}
	}

	private void digestClassLoaderConfiguration(ClassLoader classLoader, 
			ConfigurationDigester configurationDigester, 
			String currentConfigurationName, 
			ConfigurationException customClassLoaderConfigurationException) {

		long start = System.currentTimeMillis();
		if(LOG.isDebugEnabled()) LOG.debug("creating new configuration ["+currentConfigurationName+"]");

		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		String currentConfigurationVersion = null;

		if (classLoader != null) {
			Thread.currentThread().setContextClassLoader(classLoader);
			currentConfigurationVersion = ConfigurationUtils.getConfigurationVersion(classLoader);
			if(StringUtils.isEmpty(currentConfigurationVersion)) {
				LOG.info("unable to determine [configuration.version] for configuration ["+currentConfigurationName+"]");
			}
		}

		if(LOG.isDebugEnabled()) LOG.debug("configuration ["+currentConfigurationName+"] found currentConfigurationVersion ["+currentConfigurationVersion+"]");

		//TODO autowire the entire configuration in it's own context.
		Configuration configuration = createBeanAutowireByName(Configuration.class);
		try {
			configuration.setName(currentConfigurationName);
			configuration.setVersion(currentConfigurationVersion);
			configuration.setIbisManager(ibisManager);
			configuration.setClassLoader(classLoader);
			ibisManager.addConfiguration(configuration);
			if (customClassLoaderConfigurationException == null) {
				ConfigurationWarnings.getInstance().setActiveConfiguration(configuration);

				if(AppConstants.getInstance(classLoader).getBoolean("jdbc.migrator.active", false)) {
					try {
						Migrator databaseMigrator = getBean("jdbcMigrator", Migrator.class);
						databaseMigrator.setIbisContext(this);
						databaseMigrator.configure(currentConfigurationName, classLoader);
						databaseMigrator.update();
						databaseMigrator.close();
					}
					catch (Exception e) {
						log(currentConfigurationName, currentConfigurationVersion, e.getMessage(), MessageKeeperLevel.ERROR);
					}
				}

				configurationDigester.digestConfiguration(classLoader, configuration);
				if (currentConfigurationVersion == null) {
					currentConfigurationVersion = configuration.getVersion();
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
			} else {
				throw customClassLoaderConfigurationException;
			}

			LOG.info("configured configuration ["+currentConfigurationName+"] successfully");
		} catch (ConfigurationException e) {
			configuration.setConfigurationException(e);
			log(currentConfigurationName, currentConfigurationVersion, " exception", MessageKeeperLevel.ERROR, e);
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
			ConfigurationWarnings.getInstance().setActiveConfiguration(null);
		}
	}

	private void generateFlow() { //Generate big flow diagram file for all configurations
		if (flowDiagramManager != null) {
			List<Configuration> configurations = ibisManager.getConfigurations();
			try {
				flowDiagramManager.generate(configurations);
			} catch (IOException e) {
				log(ALL_CONFIGS_KEY, null, "error generating flow diagram", MessageKeeperLevel.WARN, e);
			}
		}
	}

	private void log(String message) {
		log(null, null, message, MessageKeeperLevel.INFO, null, true);
	}

	private void log(String message, MessageKeeperLevel level) {
		log(null, null, message, level, null, true);
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
			LOG.info(m, e);
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
