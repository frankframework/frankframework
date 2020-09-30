/*
   Copyright 2019-2020 WeAreFrank!

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
package nl.nn.adapterframework.lifecycle;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * Creates and maintains the (Spring) Application Context. If the context is loaded through a {@link IbisApplicationServlet servlet} 
 * it will register the servlet in the context. When the Application Context is created or destroyed it will also create/destroy the servlet.
 * This ensures that the correct {@link SpringBus bus} will be used in which CXF will register it's endpoints and dispatchers.
 * 
 * <br/><br/>
 * 
 * It is important that the Application Context is created before the {@link IbisApplicationServlet servlet} initializes. 
 * Otherwise the servlet will register under the wrong {@link SpringBus bus}!
 * 
 * <br/><br/>
 * 
 * It is possible to retrieve the Application Context through the Spring WebApplicationContextUtils class
 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
 *
 */
public class IbisApplicationContext {
	public enum BootState {
		FIRST_START,STARTING,STARTED,STOPPING,STOPPED,ERROR;
		private Exception ex = null;
		public BootState setException(Exception ex) {
			this.ex = ex;
			return this;
		}
		public Exception getException() {
			return ex;
		}
		public static BootState ERROR(Exception ex) {
			return BootState.ERROR.setException(ex);
		}
	}

	private AbstractApplicationContext applicationContext;
	private ApplicationContext parentContext = null;

	public final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private Logger log = LogUtil.getLogger(this);
	private BootState STATE = BootState.FIRST_START;
	private Map<String, String> iafModules = new HashMap<String, String>();


	public void setParentContext(ApplicationContext parentContext) {
		this.parentContext = parentContext;
	}

	/**
	 * Create Spring Bean factory.
	 *
	 * Create the Spring Bean Factory using the default <code>springContext</code>,
	 * if not <code>null</code>.
	 *
	 * @throws BeansException If the Factory can not be created.
	 *
	 */
	protected void createApplicationContext() throws BeansException {
		log.debug("creating Spring Application Context");
		if(!STATE.equals(BootState.FIRST_START))
			STATE = BootState.STARTING;

		long start = System.currentTimeMillis();

		lookupApplicationModules();

		try {
			applicationContext = createClassPathApplicationContext();
			if(parentContext != null) {
				log.debug("found Spring rootContext ["+parentContext+"]");
				applicationContext.setParent(parentContext);

				// We need to set the SpringBus here, because on WebSphere the IbisApplicationInitalizer is ran from a different ClassLoader.
				// This causes the WebServiceListeners register them selves on a different (newly created) SpringBus instead of the Ibis one.
				SpringBus bus = (SpringBus) parentContext.getBean("cxf");
				BusFactory.setDefaultBus(bus);
			}
			applicationContext.refresh();
		} catch (BeansException be) {
			STATE = BootState.ERROR(be);
			throw be;
		}

		log.info("created "+applicationContext.getClass().getSimpleName()+" in " + (System.currentTimeMillis() - start) + " ms");
		STATE = BootState.STARTED;
	}

	/**
	 * Loads springContext, springUnmanagedDeployment, springCommon and files specified by the SPRING.CONFIG.LOCATIONS
	 * property in AppConstants.properties
	 * 
	 * @param classLoader to use in order to find and validate the Spring Configuration files
	 * @return A String array containing all files to use.
	 */
	private String[] getSpringConfigurationFiles(ClassLoader classLoader) {
		List<String> springConfigurationFiles = new ArrayList<String>();
		springConfigurationFiles.add(XmlWebApplicationContext.CLASSPATH_URL_PREFIX + "/springUnmanagedDeployment.xml");
		springConfigurationFiles.add(XmlWebApplicationContext.CLASSPATH_URL_PREFIX + "/springCommon.xml");

		StringTokenizer locationTokenizer = AppConstants.getInstance().getTokenizedProperty("SPRING.CONFIG.LOCATIONS");
		while(locationTokenizer.hasMoreTokens()) {
			String file = locationTokenizer.nextToken();
			if(log.isDebugEnabled()) log.debug("found spring configuration file to load ["+file+"]");

			URL fileURL = classLoader.getResource(file);
			if(fileURL == null) {
				log.error("unable to locate Spring configuration file ["+file+"]");
			} else {
				if(file.indexOf(":") == -1) {
					file = XmlWebApplicationContext.CLASSPATH_URL_PREFIX+"/"+file;
				}

				springConfigurationFiles.add(file);
			}
		}

		log.info("loading Spring configuration files "+springConfigurationFiles+"");
		return springConfigurationFiles.toArray(new String[springConfigurationFiles.size()]);
	}

	/**
	 * Creates the Spring Application Context when ran from the command line.
	 * @throws BeansException when the Context fails to initialize
	 */
	private ClassPathXmlApplicationContext createClassPathApplicationContext() throws BeansException {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource("ibis", APP_CONSTANTS));
		applicationContext.setConfigLocations(getSpringConfigurationFiles(applicationContext.getClassLoader()));

		return applicationContext;
	}

	/**
	 * Destroys the Spring context
	 */
	protected void destroyApplicationContext() {
		if (applicationContext != null) {
			String oldContextName = applicationContext.getDisplayName();
			log.debug("destroying Ibis Application Context ["+oldContextName+"]");

			applicationContext.close();
			applicationContext = null;

			log.info("destroyed Ibis Application Context ["+oldContextName+"]");
		}
	}

	public <T> T getBean(String beanName, Class<T> beanClass) {
		return applicationContext.getBean(beanName, beanClass);
	}

	@SuppressWarnings("unchecked")
	public <T> T createBeanAutowireByName(Class<T> beanClass) {
		return (T) applicationContext.getAutowireCapableBeanFactory().createBean(beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
	}

	public void autowireBeanProperties(Object existingBean, int autowireMode, boolean dependencyCheck) {
		applicationContext.getAutowireCapableBeanFactory().autowireBeanProperties(existingBean, autowireMode, dependencyCheck);
	}

	public void initializeBean(Object existingBean, String beanName) {
		applicationContext.getAutowireCapableBeanFactory().initializeBean(existingBean, beanName);
	}

	public String[] getBeanNamesForType(Class<?> beanClass) {
		return applicationContext.getBeanNamesForType(beanClass);
	}

	public boolean isPrototype(String beanName) {
		return applicationContext.isPrototype(beanName);
	}

	/**
	 * Returns the Spring XML Bean Factory If non exists yet it will create one.
	 * If initializing the context fails, it will return null
	 * 
	 * @return Spring XML Bean Factory or NULL
	 */
	public AbstractApplicationContext getApplicationContext() {
		if(applicationContext == null)
			createApplicationContext();

		return applicationContext;
	}

	public BootState getBootState() {
		return STATE;
	}

	/**
	 * Register all IBIS modules that can be found on the classpath
	 * TODO: retrieve this (automatically/) through Spring
	 */
	private void lookupApplicationModules() {
		List<String> modulesToScanFor = new ArrayList<String>();

		modulesToScanFor.add("ibis-adapterframework-akamai");
		modulesToScanFor.add("ibis-adapterframework-cmis");
		modulesToScanFor.add("ibis-adapterframework-coolgen");
		modulesToScanFor.add("ibis-adapterframework-core");
		modulesToScanFor.add("ibis-adapterframework-ibm");
		modulesToScanFor.add("ibis-adapterframework-idin");
		modulesToScanFor.add("ibis-adapterframework-ifsa");
		modulesToScanFor.add("ibis-adapterframework-ladybug");
		modulesToScanFor.add("ibis-adapterframework-larva");
		modulesToScanFor.add("ibis-adapterframework-sap");
		modulesToScanFor.add("ibis-adapterframework-tibco");
		modulesToScanFor.add("ibis-adapterframework-webapp");

		registerApplicationModules(modulesToScanFor);
	}

	/**
	 * Register IBIS modules that can be found on the classpath
	 * @param modules list with modules to register
	 */
	private void registerApplicationModules(List<String> modules) {
		for(String module: modules) {
			String version = getModuleVersion(module);

			if(version != null) {
				iafModules.put(module, version);
				APP_CONSTANTS.put(module+".version", version);
				log.info("Loading IAF module ["+module+"] version ["+version+"]");
			}
		}
	}

	/**
	 * Get IBIS module version
	 * @param module name of the module to fetch the version
	 * @return module version or null if not found
	 */
	private String getModuleVersion(String module) {
		ClassLoader classLoader = this.getClass().getClassLoader();
		String basePath = "META-INF/maven/org.ibissource/";
		URL pomProperties = classLoader.getResource(basePath+module+"/pom.properties");

		if(pomProperties != null) {
			try {
				InputStream is = pomProperties.openStream();
				Properties props = new Properties();
				props.load(is);
				return (String) props.get("version");
			} catch (IOException e) {
				log.warn("unable to read pom.properties file for module["+module+"]", e);

				return "unknown";
			}
		}

		// unable to find module, assume it's not on the classpath
		return null;
	}
}
