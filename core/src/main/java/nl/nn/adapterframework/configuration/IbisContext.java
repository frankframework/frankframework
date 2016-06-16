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

import java.util.StringTokenizer;

import nl.nn.adapterframework.configuration.classloaders.BasePathClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.configuration.classloaders.DirectoryClassLoader;
import nl.nn.adapterframework.configuration.classloaders.JarFileClassLoader;
import nl.nn.adapterframework.configuration.classloaders.ServiceClassLoader;
import nl.nn.adapterframework.unmanaged.DefaultIbisManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.JdkVersion;
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
    private final static Logger log = LogUtil.getLogger(IbisContext.class);

	public static final String APPLICATION_SERVER_TYPE = "application.server.type";

    private ApplicationContext applicationContext;
	private static String springContextFileName = null;
    private IbisManager ibisManager;
    private static String applicationServerType = null;

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
	public boolean init() {
		initContext(getSpringContextFileName());
		AppConstants appConstants = AppConstants.getInstance();
		String configurations = appConstants.getResolvedProperty("configurations.names");
		StringTokenizer tokenizer = new StringTokenizer(configurations, ",");
		boolean configLogAppend = false;
		while (tokenizer.hasMoreTokens()) {
			String configurationName = tokenizer.nextToken();
			String configurationFile = appConstants.getResolvedProperty(
					"configurations." + configurationName + ".configurationFile");
			if (configurationFile == null) {
				configurationFile = configurationName + "/Configuration.xml";
			}
			String classLoaderType = appConstants.getResolvedProperty(
					"configurations." + configurationName + ".classLoaderType");
			ConfigurationException customClassLoaderConfigurationException = null;
			ClassLoader classLoader = null;
			try {
				if ("DirClassLoader".equals(classLoaderType)) {
					String directory = appConstants.getResolvedProperty(
							"configurations." + configurationName + ".directory");
					classLoader = new DirectoryClassLoader(directory);
				} else if ("JarFileClassLoader".equals(classLoaderType)) {
					String jar = appConstants.getResolvedProperty(
							"configurations." + configurationName + ".jar");
					classLoader = new JarFileClassLoader(jar, configurationName);
				} else if ("ServiceClassLoader".equals(classLoaderType)) {
					String adapterName = appConstants.getResolvedProperty(
							"configurations." + configurationName + ".adapterName");
					classLoader = new ServiceClassLoader(ibisManager, adapterName, configurationName);
				} else if ("DatabaseClassLoader".equals(classLoaderType)) {
					classLoader = new DatabaseClassLoader(this, configurationName);
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
			configuration.setName(configurationName);
			configuration.setIbisManager(ibisManager);
			((DefaultIbisManager)ibisManager).addConfiguration(configuration);
			ConfigurationWarnings.getInstance().setActiveConfiguration(configuration);
			try {
				if (customClassLoaderConfigurationException == null) {
					ConfigurationDigester configurationDigester = new ConfigurationDigester();
					configurationDigester.digestConfiguration(classLoader, configuration, configurationFile, configLogAppend);
					configLogAppend = true;
					if (configuration.isAutoStart()) {
						ibisManager.startConfiguration(configuration);
					}
				} else {
					throw customClassLoaderConfigurationException;
				}
			} catch (ConfigurationException e) {
				configuration.setConfigurationException(e);
				log.error("Configuration exception loading: " + configurationFile, e);
			} finally {
				Thread.currentThread().setContextClassLoader(originalClassLoader);
				ConfigurationWarnings.getInstance().setActiveConfiguration(null);
			}
		}
		log.info("* IBIS Startup: Startup complete");
		return true;
	}

	public void initContext(String springContext) {
		log.info("* IBIS Startup: Running on JDK version [" + System.getProperty("java.version")
				+ "], Spring indicates JDK Major version: 1." + (JdkVersion.getMajorJavaVersion()+3));
		// This should be made conditional, somehow
//		startJmxServer();

		applicationContext = createApplicationContext(springContext);
		ibisManager = (IbisManager) applicationContext.getBean("ibisManager");
		ibisManager.setIbisContext(this);
		AbstractSpringPoweredDigesterFactory.setIbisContext(this);
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
	private static ApplicationContext createApplicationContext(String springContext) throws BeansException {
		// Reading in Spring Context
		if (springContext == null) {
		    springContext = getSpringContextFileName();
		}
		log.info("* IBIS Startup: Creating Spring ApplicationContext from file [" + springContext + "]");
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource("ibis", AppConstants.getInstance()));
		applicationContext.setConfigLocation(springContext);
		applicationContext.refresh();
		return applicationContext;
	}

	public void destroyConfig() {
		((ConfigurableApplicationContext)applicationContext).close();
	}

    /**
     * @since 5.0.29
     */
    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }


//	public Object getAutoWiredObject(Class clazz) throws ConfigurationException {
//		return getAutoWiredObject(clazz, null);
//	}
//
//	public Object getAutoWiredObject(Class clazz, String prototypeName) throws ConfigurationException {
//
//		String beanName;
//
//		prototypeName="proto-"+prototypeName;
//		// No explicit classname given; get bean from Spring Factory
//		if (clazz == null) {
//			beanName = prototypeName;
//		} else {
//			// Get all beans matching the classname given
//			String[] matchingBeans = getBeanFactory().getBeanNamesForType(clazz);
//			if (matchingBeans.length == 1) {
//				// Only 1 bean of this type, so create it
//				beanName = matchingBeans[0];
//			} else if (matchingBeans.length > 1) {
//				// multiple beans; find if there's one with the
//				// same name as from 'getBeanName'.
//				beanName = prototypeName;
//			} else {
//				// No beans matching the type.
//				// Create instance, and if the instance implements
//				// Spring's BeanFactoryAware interface, use it to
//				// set BeanFactory attribute on this Bean.
//				try {
//					return createBeanAndAutoWire(clazz, prototypeName);
//				} catch (Exception e) {
//					throw new ConfigurationException(e);
//				}
//			}
//		}
//
//		// Only accept prototype-beans!
//		if (!getBeanFactory().isPrototype(beanName)) {
//			throw new ConfigurationException("Beans created from the BeanFactory must be prototype-beans, bean ["
//				+ beanName + "] of class [" + clazz.getName() + "] is not.");
//		}
//		if (log.isDebugEnabled()) {
//			log.debug("Creating bean with actual bean-name [" + beanName + "], bean-class [" + (clazz != null ? clazz.getName() : "null") + "] from Spring Bean Factory.");
//		}
//		return getBeanFactory().getBean(beanName, clazz);
//	}

//	protected Object createBeanAndAutoWire(Class beanClass, String prototype) throws InstantiationException, IllegalAccessException {
//		if (log.isDebugEnabled()) {
//			log.debug("Bean class [" + beanClass.getName() + "] not found in Spring Bean Factory, instantiating directly and using Spring Factory for auto-wiring support.");
//		}
//		Object o = beanClass.newInstance();
//		if (getBeanFactory() instanceof AutowireCapableBeanFactory) {
//			((AutowireCapableBeanFactory)getBeanFactory()).autowireBeanProperties(o,AutowireCapableBeanFactory.AUTOWIRE_BY_NAME,false);
//			o = ((AutowireCapableBeanFactory)getBeanFactory()).initializeBean(o, prototype);
//		} else if (o instanceof BeanFactoryAware) {
//			((BeanFactoryAware)o).setBeanFactory(getBeanFactory());
//		}
//		return o;
//	}

//	private void startJmxServer() {
//		//Start MBean server
//
//        // It seems that no reference to the server is required anymore,
//        // anywhere later? So no reference is returned from
//        // this method.
//        log.info("* IBIS Startup: Attempting to start MBean server");
//		MBeanServer server=MBeanServerFactory.createMBeanServer();
//		try {
//		  ObjectInstance html = server.createMBean("com.sun.jdmk.comm.HtmlAdaptorServer", null);
//
//		  server.invoke(html.getObjectName(), "start", new Object[0], new String[0]);
//        } catch (ReflectionException e ) {
//            log.error("Requested JMX Server MBean can not be created; JMX not available.");
//            return;
//        } catch (Exception e) {
//		    log.error("Error with jmx:",e);
//            return;
//		}
//		log.info("MBean server up and running. Monitor your application by pointing your browser to http://localhost:8082");
//	}

//	public void setBeanFactory(ListableBeanFactory factory) {
//		beanFactory = factory;
//	}


	private static String getSpringContextFileName() {
		if (springContextFileName == null) {
			springContextFileName = "/springContext" + getApplicationServerType() + ".xml";
		}
		return springContextFileName;
	}

	public IbisManager getIbisManager() {
		return ibisManager;
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

	public static void setApplicationServerType (String string) {
		applicationServerType = string;
	}
	
	public static String getApplicationServerType() {
		if (applicationServerType == null) {
			applicationServerType = AppConstants.getInstance().getString(
					APPLICATION_SERVER_TYPE, null);
			if (applicationServerType != null) {
				if (applicationServerType.equalsIgnoreCase("WAS5")
						|| applicationServerType.equalsIgnoreCase("WAS6")) {
					ConfigurationWarnings configWarnings = ConfigurationWarnings
							.getInstance();
					String msg = "implementing value [" + applicationServerType
							+ "] of property [" + APPLICATION_SERVER_TYPE
							+ "] as [WAS]";
					configWarnings.add(log, msg);
					applicationServerType = "WAS";
				} else if (applicationServerType.equalsIgnoreCase("TOMCAT6")) {
					ConfigurationWarnings configWarnings = ConfigurationWarnings
							.getInstance();
					String msg = "implementing value [" + applicationServerType
							+ "] of property [" + APPLICATION_SERVER_TYPE
							+ "] as [TOMCAT]";
					configWarnings.add(log, msg);
					applicationServerType = "TOMCAT";
				}
			}
		}
		return applicationServerType;
	}
}
