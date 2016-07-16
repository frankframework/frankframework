/*
   Copyright 2013, 2016 Nationale-Nederlanden

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

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.configuration.classloaders.ServiceClassLoader;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageKeeper;
import nl.nn.adapterframework.util.MessageKeeperMessage;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;

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
public class IbisContext {
	private final static Logger LOG = LogUtil.getLogger(IbisContext.class);
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final String INSTANCE_NAME = APP_CONSTANTS.getResolvedProperty("instance.name");
	private static final String CONFIGURATIONS = APP_CONSTANTS.getResolvedProperty("configurations.names.application");
	private static final String APPLICATION_SERVER_TYPE_PROPERTY = "application.server.type";
	private static final String APPLICATION_SERVER_TYPE;
	static {
		String applicationServerType = APP_CONSTANTS.getString(
				APPLICATION_SERVER_TYPE_PROPERTY, null);
		if (StringUtils.isNotEmpty(applicationServerType)) {
			if (applicationServerType.equalsIgnoreCase("WAS5")
					|| applicationServerType.equalsIgnoreCase("WAS6")) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings
						.getInstance();
				String msg = "implementing value [" + applicationServerType
						+ "] of property [" + APPLICATION_SERVER_TYPE_PROPERTY
						+ "] as [WAS]";
				configWarnings.add(LOG, msg);
				applicationServerType = "WAS";
			} else if (applicationServerType.equalsIgnoreCase("TOMCAT6")) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings
						.getInstance();
				String msg = "implementing value [" + applicationServerType
						+ "] of property [" + APPLICATION_SERVER_TYPE_PROPERTY
						+ "] as [TOMCAT]";
				configWarnings.add(LOG, msg);
				applicationServerType = "TOMCAT";
			}
			APPLICATION_SERVER_TYPE = applicationServerType;
		} else {
			APPLICATION_SERVER_TYPE = null;
		}
	}
	private static String applicationServerType = APPLICATION_SERVER_TYPE;
	private ApplicationContext applicationContext;
	private IbisManager ibisManager;
	private Map<String, MessageKeeper> messageKeepers = new HashMap<String, MessageKeeper>();
	private int messageKeeperSize = 10;

	/**
	 * Creates the Spring context, and load the configuration. Optionally  with
	 * a specific ClassLoader which might for example override the getResource
	 * method to load configuration and related resources from a different
	 * location from the standard classpath. In case basePath is not null the
	 * ClassLoader is wrapped in {@link BasePathClassLoader} to make it possible
	 * to reference resources in the configuration relative to the configuration
	 * file and have an extra resource override (resource is first resolved
	 * relative to the configuration, when not found it is resolved by the
	 * original ClassLoader.
	 * 
	 * @see ClassUtils#getResourceURL(ClassLoader, String)
	 * @see AppConstants#getInstance(ClassLoader)
	 */
	public synchronized void init() {
		log("startup " + getVersionInfo());
		applicationContext = createApplicationContext(getSpringContextFileName());
		ibisManager = (IbisManager)applicationContext.getBean("ibisManager");
		ibisManager.setIbisContext(this);
		AbstractSpringPoweredDigesterFactory.setIbisContext(this);
		load(null);
		log("startup " + getVersionInfo() + " complete");
	}

	public synchronized void destroy() {
		log("shutdown");
		ibisManager.shutdown();
		destroyApplicationContext();
		log("shutdown complete");
	}

	public synchronized void reload(String configurationName) {
		ibisManager.unload(configurationName);
		log(configurationName, "unload complete");
		load(configurationName);
	}

	public synchronized void fullReload() {
		destroy();
		init();
	}

	/**
	 * Create Spring Bean factory. Parameter 'springContext' can be null.
	 *
	 * Create the Spring Bean Factory using the supplied <code>springContext</code>,
	 * if not <code>null</code>.
	 *
	 * @param springContext Spring Context to create. If <code>null</code>,
	 * use the default spring context.
	 * The spring context is loaded as a spring ClassPathResource from
	 * the class path.
	 *
	 * @return The Spring XML Bean Factory.
	 * @throws BeansException If the Factory can not be created.
	 *
	 */
	private ApplicationContext createApplicationContext(String springContext) throws BeansException {
		// Reading in Spring Context
		if (springContext == null) {
			springContext = getSpringContextFileName();
		}
		log("create Spring ApplicationContext from file: " + springContext);
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource("ibis", APP_CONSTANTS));
		applicationContext.setConfigLocation(springContext);
		applicationContext.refresh();
		log("create Spring ApplicationContext complete");
		return applicationContext;
	}

	private void destroyApplicationContext() {
		((ConfigurableApplicationContext)applicationContext).close();
	}

	private void load(String configurationName) {
		boolean configFound = false;
		boolean configLogAppend = false;
		StringTokenizer tokenizer = new StringTokenizer(CONFIGURATIONS, ",");
		while (tokenizer.hasMoreTokens()) {
			String currentConfigurationName = tokenizer.nextToken();
			if (configurationName == null
					|| configurationName.equals(currentConfigurationName)) {
				configFound = true;
				String configurationFile = APP_CONSTANTS.getResolvedProperty(
						"configurations." + currentConfigurationName + ".configurationFile");
				if (configurationFile == null) {
					configurationFile = "Configuration.xml";
					if (!currentConfigurationName.equals(INSTANCE_NAME)) {
						configurationFile = currentConfigurationName + "/" + configurationFile;
					}
				}
				ClassLoader classLoader = null;
				ConfigurationException customClassLoaderConfigurationException = null;
				String classLoaderType = APP_CONSTANTS.getResolvedProperty(
						"configurations." + currentConfigurationName + ".classLoaderType");
				try {
					if ("DirectoryClassLoader".equals(classLoaderType)) {
						String directory = APP_CONSTANTS.getResolvedProperty(
								"configurations." + currentConfigurationName + ".directory");
						classLoader = new DirectoryClassLoader(directory);
					} else if ("JarFileClassLoader".equals(classLoaderType)) {
						String jar = APP_CONSTANTS.getResolvedProperty(
								"configurations." + currentConfigurationName + ".jar");
						classLoader = new JarFileClassLoader(jar, currentConfigurationName);
					} else if ("ServiceClassLoader".equals(classLoaderType)) {
						String adapterName = APP_CONSTANTS.getResolvedProperty(
								"configurations." + currentConfigurationName + ".adapterName");
						classLoader = new ServiceClassLoader(ibisManager, adapterName, currentConfigurationName);
					} else if ("DatabaseClassLoader".equals(classLoaderType)) {
						classLoader = new DatabaseClassLoader(this, currentConfigurationName);
					} else if (classLoaderType != null) {
						throw new ConfigurationException("Invalid classLoaderType: " + classLoaderType);
					}
				} catch (ConfigurationException e) {
					customClassLoaderConfigurationException = e;
				}
				String basePath = null;
				int i = configurationFile.lastIndexOf('/');
				if (i != -1) {
					basePath = configurationFile.substring(0, i + 1);
				}
				ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
				if (basePath != null) {
					if (classLoader != null) {
						classLoader = new BasePathClassLoader(classLoader, basePath);
					} else {
						classLoader = new BasePathClassLoader(originalClassLoader, basePath);
					}
				}
				if (classLoader != null) {
					Thread.currentThread().setContextClassLoader(classLoader);
				}
				Configuration configuration = new Configuration(new BasicAdapterServiceImpl());
				configuration.setName(currentConfigurationName);
				configuration.setIbisManager(ibisManager);
				ibisManager.addConfiguration(configuration);
				ConfigurationWarnings.getInstance().setActiveConfiguration(configuration);
				try {
					if (customClassLoaderConfigurationException == null) {
						ConfigurationDigester configurationDigester = new ConfigurationDigester();
						configurationDigester.digestConfiguration(classLoader, configuration, configurationFile, configLogAppend);
						configLogAppend = true;
						log(currentConfigurationName, "successfully configured");
						if (configuration.isAutoStart()) {
							ibisManager.startConfiguration(configuration);
							log(currentConfigurationName, "startup complete");
						}
					} else {
						throw customClassLoaderConfigurationException;
					}
					if (!currentConfigurationName.equals(configuration.getName())) {
						log(currentConfigurationName,
								"configuration name doesn't match Configuration name attribute: "
								+ configuration.getName(),
								MessageKeeperMessage.WARN_LEVEL);
						messageKeepers.put(configuration.getName(),
								messageKeepers.remove(currentConfigurationName));
					}
				} catch (ConfigurationException e) {
					configuration.setConfigurationException(e);
					log(currentConfigurationName, " exception",
							MessageKeeperMessage.ERROR_LEVEL, e);
				} finally {
					Thread.currentThread().setContextClassLoader(originalClassLoader);
					ConfigurationWarnings.getInstance().setActiveConfiguration(null);
				}
			}
		}
		if (!configFound) {
			log(configurationName, configurationName + " not found in '"
					+ CONFIGURATIONS + "'", MessageKeeperMessage.ERROR_LEVEL);
		}
	}

	private void log(String message) {
		log(null, message, true);
	}

	private void log(String configurationName, String message) {
		log(configurationName, message, MessageKeeperMessage.INFO_LEVEL);
	}

	private void log(String configurationName, String message, boolean allOnly) {
		log(configurationName, message, MessageKeeperMessage.INFO_LEVEL, null, allOnly);
	}

	private void log(String configurationName, String message, String level) {
		log(configurationName, message, level, null, false);
	}

	private void log(String configurationName, String message, String level, Exception e) {
		log(configurationName, message, level, e, false);
	}

	private void log(String configurationName, String message, String level, Exception e, boolean allOnly) {
		String key;
		if (allOnly || configurationName == null) {
			key = "*ALL*";
		} else {
			key = configurationName;
		}
		MessageKeeper messageKeeper = messageKeepers.get(key);
		if (messageKeeper == null) {
			messageKeeper = new MessageKeeper(messageKeeperSize < 1 ? 1 : messageKeeperSize);
			messageKeepers.put(key, messageKeeper);
		}
		String m;
		if (configurationName != null) {
			m = "Configuration [" + configurationName + "] " + message;
		} else {
			m = "IAF [" + INSTANCE_NAME + "] " + message;
		}
		if (level.equals(MessageKeeperMessage.ERROR_LEVEL)) {
			LOG.info(m, e);
		} else if (level.equals(MessageKeeperMessage.WARN_LEVEL)) {
			LOG.warn(m, e);
		} else {
			LOG.info(m, e);
		}
		if (e != null) {
			m = m + ": " + e.getMessage();
		}
		messageKeeper.add(m, level);
		if (!allOnly) {
			log(configurationName, message, level, e, true);
		}
	}

	/**
	 * Get MessageKeeper for a specific configuration. The MessageKeeper is not
	 * stored at the Configuration object instance to prevent messages being
	 * lost after configuration reload.
	 */
	public MessageKeeper getMessageKeeper(String configurationName) {
		return messageKeepers.get(configurationName);
	}

	public IbisManager getIbisManager() {
		return ibisManager;
	}

	public String getVersionInfo() {
		String versionInfo = APP_CONSTANTS.getProperty("application.name") + " "
				+ APP_CONSTANTS.getProperty("application.version") + " "
				+ APP_CONSTANTS.getProperty("instance.name") + " "
				+ APP_CONSTANTS.getProperty("instance.version");
		String buildId = APP_CONSTANTS.getProperty("instance.build_id");
		if (StringUtils.isNotEmpty(buildId)) {
			versionInfo += " " + buildId;
		}
		return versionInfo;
	}

	public static void main(String[] args) {
		IbisContext ibisContext = new IbisContext();
		ibisContext.init();
	}

	public String getSpringContextFileName() {
		return "/springContext" + getApplicationServerType() + ".xml";
	}

	@SuppressWarnings("static-access")
	public void setApplicationServerType(String applicationServerType) {
		if (applicationServerType.equals(APPLICATION_SERVER_TYPE)) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings
					.getInstance();
			String msg = "value [" + applicationServerType
					+ "] of property ["
					+ APPLICATION_SERVER_TYPE_PROPERTY
					+ "] is already the retrieved value";
			configWarnings.add(LOG, msg);
		}
		this.applicationServerType = applicationServerType;
	}

	public static String getApplicationServerType() {
		return applicationServerType;
	}

	public Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}

	public Object getBean(String beanName, Class beanClass) {
		return applicationContext.getBean(beanName, beanClass);
	}

	public Object createBeanAutowireByName(Class beanClass) {
		return applicationContext.getAutowireCapableBeanFactory().createBean(
				beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) {
		applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(existingBean, autowireMode, dependencyCheck);
	}

	public void initializeBean(Object existingBean, String beanName) {
		applicationContext.getAutowireCapableBeanFactory().initializeBean(existingBean, beanName);
	}

	public String[] getBeanNamesForType(Class beanClass) {
		return applicationContext.getBeanNamesForType(beanClass);
	}

	public boolean isPrototype(String beanName) {
		return applicationContext.isPrototype(beanName);
	}
}
