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

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.configuration.classloaders.ReloadAware;
import nl.nn.adapterframework.configuration.classloaders.ServiceClassLoader;
import nl.nn.adapterframework.core.Adapter;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.http.RestServiceDispatcher;
import nl.nn.adapterframework.receivers.JavaListener;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.FlowDiagram;
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
	private static final String FLOW_CREATE_DIAGRAM_URL = APP_CONSTANTS.getResolvedProperty("flow.create.url");
	private static final long UPTIME = System.currentTimeMillis();
	static {
		String applicationServerType = System.getProperty(
				APPLICATION_SERVER_TYPE_PROPERTY);
		if (StringUtils.isNotEmpty(applicationServerType)) {
			if (applicationServerType.equalsIgnoreCase("WAS5")
					|| applicationServerType.equalsIgnoreCase("WAS6")) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings
						.getInstance();
				String msg = "implementing value [" + applicationServerType
						+ "] of property [" + APPLICATION_SERVER_TYPE_PROPERTY
						+ "] as [WAS]";
				configWarnings.add(LOG, msg);
				System.setProperty(APPLICATION_SERVER_TYPE_PROPERTY, "WAS");
			} else if (applicationServerType.equalsIgnoreCase("TOMCAT6")) {
				ConfigurationWarnings configWarnings = ConfigurationWarnings
						.getInstance();
				String msg = "implementing value [" + applicationServerType
						+ "] of property [" + APPLICATION_SERVER_TYPE_PROPERTY
						+ "] as [TOMCAT]";
				configWarnings.add(LOG, msg);
				System.setProperty(APPLICATION_SERVER_TYPE_PROPERTY, "TOMCAT");
			}
		}
	}
	private ApplicationContext applicationContext;
	private IbisManager ibisManager;
	private Map<String, ClassLoader> classLoaders = new HashMap<String, ClassLoader>();
	private Map<String, MessageKeeper> messageKeepers = new HashMap<String, MessageKeeper>();
	private int messageKeeperSize = 10;
	private FlowDiagram flowDiagram;

	public void setDefaultApplicationServerType(String defaultApplicationServerType) {
		if (defaultApplicationServerType.equals(getApplicationServerType())) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings
					.getInstance();
			String msg = "property ["
					+ APPLICATION_SERVER_TYPE_PROPERTY
					+ "] already has a default value ["
					+ defaultApplicationServerType + "]";
			configWarnings.add(LOG, msg);
		} else if (StringUtils.isEmpty(getApplicationServerType())) {
			APP_CONSTANTS.setProperty(APPLICATION_SERVER_TYPE_PROPERTY, defaultApplicationServerType);
		}
	}

	public static String getApplicationServerType() {
		return APP_CONSTANTS.getResolvedProperty(APPLICATION_SERVER_TYPE_PROPERTY);
	}

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
		long start = System.currentTimeMillis();
		if (StringUtils.isNotEmpty(FLOW_CREATE_DIAGRAM_URL)) {
			flowDiagram = new FlowDiagram(FLOW_CREATE_DIAGRAM_URL);
		}
		applicationContext = createApplicationContext();
		ibisManager = (IbisManager)applicationContext.getBean("ibisManager");
		ibisManager.setIbisContext(this);
		AbstractSpringPoweredDigesterFactory.setIbisContext(this);
		load(null);
		log("startup in " + (System.currentTimeMillis() - start) + " ms");
	}

	public synchronized void destroy() {
		long start = System.currentTimeMillis();
		ibisManager.shutdown();
		destroyApplicationContext();
		log("shutdown in " + (System.currentTimeMillis() - start) + " ms");
	}

	public synchronized void reload(String configurationName) {
		Configuration configuration = ibisManager.getConfiguration(configurationName);
		if (configuration != null) {
			long start = System.currentTimeMillis();
			ibisManager.unload(configurationName);
			if (configuration.getAdapterService().getAdapters().size() > 0) {
				log("Not all adapters are unregistered: "
						+ configuration.getAdapterService().getAdapters(),
						MessageKeeperMessage.ERROR_LEVEL);
			}
			// Improve configuration reload performance. Probably because
			// garbage collection will be easier.
			configuration.setAdapterService(null);
			String configurationVersion = configuration.getVersion();
			log(configurationName, configurationVersion, "unload in "
					+ (System.currentTimeMillis() - start) + " ms");
		} else {
			log("Configuration [" + configurationName + "] to unload not found",
					MessageKeeperMessage.WARN_LEVEL);
		}
		load(configurationName);
	}

	public synchronized void fullReload() {
		destroy();
		Set<String> javaListenerNames = JavaListener.getListenerNames();
		if (javaListenerNames.size() > 0) {
			log("Not all java listeners are unregistered: " + javaListenerNames,
					MessageKeeperMessage.ERROR_LEVEL);
		}
		Set uriPatterns = RestServiceDispatcher.getInstance().getUriPatterns();
		if (uriPatterns.size() > 0) {
			log("Not all rest listeners are unregistered: " + uriPatterns,
					MessageKeeperMessage.ERROR_LEVEL);
		}
		Set mbeans = JmxMbeanHelper.getMBeans();
		if (mbeans != null && mbeans.size() > 0) {
			log("Not all JMX MBeans are unregistered: " + mbeans,
					MessageKeeperMessage.ERROR_LEVEL);
		}
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
	private ApplicationContext createApplicationContext() throws BeansException {
		// Reading in Spring Context
		long start = System.currentTimeMillis();
		String springContext = "/springContext.xml";
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource("ibis", APP_CONSTANTS));
		applicationContext.setConfigLocation(springContext);
		applicationContext.refresh();
		log("startup " + springContext + " in "
				+ (System.currentTimeMillis() - start) + " ms");
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
				long start = System.currentTimeMillis();
				configFound = true;
				String configurationFile = APP_CONSTANTS.getResolvedProperty(
						"configurations." + currentConfigurationName + ".configurationFile");
				if (configurationFile == null) {
					configurationFile = "Configuration.xml";
					if (!currentConfigurationName.equals(INSTANCE_NAME)) {
						configurationFile = currentConfigurationName + "/" + configurationFile;
					}
				}
				ConfigurationException customClassLoaderConfigurationException = null;
				ClassLoader classLoader = classLoaders.get(currentConfigurationName);
				if (classLoader != null) {
					// Reuse class loader as it is difficult to have all
					// references to the class loader removed (see also
					// http://zeroturnaround.com/rebellabs/rjc201/).
					// Create a heapdump after an unload and garbage collect and
					// view the references to the instances of the root class
					// loader class (BasePathClassLoader when a base path is
					// used).
					if (classLoader instanceof ReloadAware) {
						try {
							((ReloadAware)classLoader).reload();
						} catch (ConfigurationException e) {
							customClassLoaderConfigurationException = e;
						}
					}
				} else {
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
					if (basePath != null) {
						if (classLoader != null) {
							classLoader = new BasePathClassLoader(classLoader, basePath);
						} else {
							classLoader = new BasePathClassLoader(
									Thread.currentThread().getContextClassLoader(),
									basePath);
						}
					}
					classLoaders.put(currentConfigurationName, classLoader);
				}
				String currentConfigurationVersion = null;
				URL buildInfoUrl = ClassUtils.getResourceURL(classLoader, "BuildInfo.properties");
				if (buildInfoUrl != null) {
					Properties buildInfoPproperties = new Properties();
					try {
						buildInfoPproperties.load(buildInfoUrl.openStream());
						currentConfigurationVersion = getConfigurationVersion(buildInfoPproperties);
					} catch (IOException e) {
						log(currentConfigurationName, currentConfigurationVersion, "error reading [BuildInfo.properties]", MessageKeeperMessage.ERROR_LEVEL, e);
					}
				}
				Configuration configuration = null;
				ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
				if (classLoader != null) {
					Thread.currentThread().setContextClassLoader(classLoader);
				}
				try {
					configuration = new Configuration(new BasicAdapterServiceImpl());
					configuration.setName(currentConfigurationName);
					configuration.setVersion(currentConfigurationVersion);
					configuration.setIbisManager(ibisManager);
					ibisManager.addConfiguration(configuration);
					ConfigurationWarnings.getInstance().setActiveConfiguration(configuration);
					if (customClassLoaderConfigurationException == null) {
						ConfigurationDigester configurationDigester = new ConfigurationDigester();
						configurationDigester.digestConfiguration(classLoader, configuration, configurationFile, configLogAppend);
						configLogAppend = true;
						if (currentConfigurationVersion == null) {
							currentConfigurationVersion = configuration.getVersion();
						} else if (!currentConfigurationVersion.equals(configuration.getVersion())) {
							log(currentConfigurationName, currentConfigurationVersion,
									"configuration version doesn't match Configuration version attribute: "
									+ configuration.getVersion(),
									MessageKeeperMessage.WARN_LEVEL);
						}
						if (!currentConfigurationName.equals(configuration.getName())) {
							log(currentConfigurationName, currentConfigurationVersion,
									"configuration name doesn't match Configuration name attribute: "
									+ configuration.getName(),
									MessageKeeperMessage.WARN_LEVEL);
							messageKeepers.put(configuration.getName(),
									messageKeepers.remove(currentConfigurationName));
						}
						if (configuration.isAutoStart()) {
							ibisManager.startConfiguration(configuration);
							log(currentConfigurationName, currentConfigurationVersion,
									"startup in " + (System.currentTimeMillis() - start) + " ms");
						} else {
							log(currentConfigurationName, currentConfigurationVersion,
									"configured in " + (System.currentTimeMillis() - start) + " ms");
						}
						generateFlows(configuration, currentConfigurationName, currentConfigurationVersion);
					} else {
						throw customClassLoaderConfigurationException;
					}
				} catch (ConfigurationException e) {
					configuration.setConfigurationException(e);
					log(currentConfigurationName, currentConfigurationVersion, " exception",
							MessageKeeperMessage.ERROR_LEVEL, e);
				} finally {
					Thread.currentThread().setContextClassLoader(originalClassLoader);
					ConfigurationWarnings.getInstance().setActiveConfiguration(null);
				}
			}
		}
		generateFlow();
		if (!configFound) {
			log(configurationName, configurationName + " not found in '"
					+ CONFIGURATIONS + "'", MessageKeeperMessage.ERROR_LEVEL);
		}
	}

	private void generateFlows(Configuration configuration,
			String currentConfigurationName, String currentConfigurationVersion) {
		if (flowDiagram != null) {
			List<IAdapter> registeredAdapters = configuration
					.getRegisteredAdapters();
			for (Iterator adapterIt = registeredAdapters.iterator(); adapterIt
					.hasNext();) {
				Adapter adapter = (Adapter) adapterIt.next();
				try {
					flowDiagram.generate(adapter);
				} catch (Exception e) {
					log(currentConfigurationName, currentConfigurationVersion,
							"error generating flowDiagram for adapter ["
									+ adapter.getName() + "]",
							MessageKeeperMessage.WARN_LEVEL, e);
				}
			}

			try {
				flowDiagram.generate(configuration);
			} catch (Exception e) {
				log(currentConfigurationName, currentConfigurationVersion,
						"error generating flowDiagram for configuration ["
								+ configuration.getName() + "]",
						MessageKeeperMessage.WARN_LEVEL, e);
			}
		}
	}

	private void generateFlow() {
		if (flowDiagram != null) {
			List<Configuration> configurations = ibisManager
					.getConfigurations();
			try {
				flowDiagram.generate(configurations);
			} catch (Exception e) {
				log("*ALL*", null, "error generating flowDiagram",
						MessageKeeperMessage.WARN_LEVEL, e);
			}
		}
	}

	private void log(String message) {
		log(null, null, message, MessageKeeperMessage.INFO_LEVEL, null, true);
	}

	private void log(String message, String level) {
		log(null, null, message, level, null, true);
	}

	private void log(String configurationName, String configurationVersion, String message) {
		log(configurationName, configurationVersion, message, MessageKeeperMessage.INFO_LEVEL);
	}

	private void log(String configurationName, String configurationVersion, String message, String level) {
		log(configurationName, configurationVersion, message, level, null, false);
	}

	private void log(String configurationName, String configurationVersion, String message, String level, Exception e) {
		log(configurationName, configurationVersion, message, level, e, false);
	}

	private void log(String configurationName, String configurationVersion, String message, String level, Exception e, boolean allOnly) {
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
			log(configurationName, configurationVersion, message, level, e, true);
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

	public String getApplicationName() {
		return APP_CONSTANTS.getProperty("instance.name", null);
	}

	public String getApplicationVersion() {
		return getVersion(APP_CONSTANTS, "instance.version", "instance.build_id");
	}

	public String getFrameworkVersion() {
		return APP_CONSTANTS.getProperty("application.version", null);
	}

	public String getConfigurationVersion(Properties properties) {
		return getVersion(properties, "configuration.version", "configuration.timestamp");
	}

	public String getVersion(Properties properties, String versionKey, String timestampKey) {
		String version = null;
		if (StringUtils.isNotEmpty(properties.getProperty(versionKey))) {
			version = properties.getProperty(versionKey);
			if (StringUtils.isNotEmpty(properties.getProperty(timestampKey))) {
				version = version + "_" + properties.getProperty(timestampKey);
			}
		}
		return version;
	}
	
	public Date getUptimeDate() {
		return new Date(UPTIME);
	}
	
	public String getUptime() {
		return getUptime(DateUtils.FORMAT_GENERICDATETIME);
	}
	
	public String getUptime(String dateFormat) {
		return DateUtils.format(getUptimeDate(), dateFormat);
	}

	public static void main(String[] args) {
		IbisContext ibisContext = new IbisContext();
		ibisContext.init();
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
