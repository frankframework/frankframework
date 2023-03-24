/*
   Copyright 2019-2021 WeAreFrank!

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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ResourceUtils;

import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;

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
public class IbisApplicationContext implements Closeable {
	private Exception startupException;

	public enum BootState {
		FIRST_START, STARTING, STARTED, STOPPING, STOPPED, ERROR;

		public boolean isIdle() {
			return !this.equals(STARTING) || !this.equals(STOPPING);
		}
		public boolean inError() {
			return this.equals(ERROR);
		}
	}

	private AbstractApplicationContext applicationContext;
	private ApplicationContext parentContext = null;

	protected static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private Logger log = LogUtil.getLogger(this);
	private BootState state = BootState.FIRST_START;
	private Map<String, String> iafModules = new HashMap<>();


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
		if(!state.equals(BootState.FIRST_START)) {
			state = BootState.STARTING;
		}
		if(startupException != null) {
			startupException = null;
		}

		long start = System.currentTimeMillis();

		lookupApplicationModules();

		try {
			applicationContext = createClassPathApplicationContext();
			if(parentContext != null) {
				log.debug("found Spring rootContext ["+parentContext+"]");
				applicationContext.setParent(parentContext);
			}
			applicationContext.refresh();
		} catch (BeansException be) {
			state = BootState.ERROR;
			startupException = be; //Save this in case we might need it later
			throw be;
		}

		log.info("created "+applicationContext.getClass().getSimpleName()+" in " + (System.currentTimeMillis() - start) + " ms");
		state = BootState.STARTED;
	}

	/**
	 * Loads springUnmanagedDeployment, SpringApplicationContext and files specified by the SPRING.CONFIG.LOCATIONS
	 * property in AppConstants.properties
	 * 
	 * @param classLoader to use in order to find and validate the Spring Configuration files
	 * @return A String array containing all files to use.
	 */
	private String[] getSpringConfigurationFiles(ClassLoader classLoader) {
		List<String> springConfigurationFiles = new ArrayList<>();
		springConfigurationFiles.add(SpringContextScope.APPLICATION.getContextFile());

		StringTokenizer locationTokenizer = AppConstants.getInstance().getTokenizedProperty("SPRING.CONFIG.LOCATIONS");
		while(locationTokenizer.hasMoreTokens()) {
			String file = locationTokenizer.nextToken();
			if(log.isDebugEnabled()) log.debug("found spring configuration file to load ["+file+"]");

			URL fileURL = classLoader.getResource(file);
			if(fileURL == null) {
				log.error("unable to locate Spring configuration file ["+file+"]");
			} else {
				if(file.indexOf(":") == -1) {
					file = ResourceUtils.CLASSPATH_URL_PREFIX+"/"+file;
				}

				springConfigurationFiles.add(file);
			}
		}

		addJmxConfigurationIfEnabled(springConfigurationFiles);

		log.info("loading Spring configuration files "+springConfigurationFiles+"");
		return springConfigurationFiles.toArray(new String[springConfigurationFiles.size()]);
	}

	private void addJmxConfigurationIfEnabled(List<String> springConfigurationFiles) {
		boolean jmxEnabled = AppConstants.getInstance().getBoolean("management.endpoints.jmx.enabled", false);
		if(jmxEnabled) {
			springConfigurationFiles.add(ResourceUtils.CLASSPATH_URL_PREFIX+"/"+"SpringApplicationContextJMX.xml");
		}
	}

	/**
	 * Creates the Spring Application Context when ran from the command line.
	 * @throws BeansException when the Context fails to initialize
	 */
	private ClassPathXmlApplicationContext createClassPathApplicationContext() {
		ClassPathXmlApplicationContext classPathapplicationContext = new ClassPathXmlApplicationContext();

		MutablePropertySources propertySources = classPathapplicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource(SpringContextScope.APPLICATION.getFriendlyName(), APP_CONSTANTS));
		classPathapplicationContext.setConfigLocations(getSpringConfigurationFiles(classPathapplicationContext.getClassLoader()));
		String instanceName = APP_CONSTANTS.getResolvedProperty("instance.name");
		classPathapplicationContext.setId(instanceName);
		classPathapplicationContext.setDisplayName("IbisApplicationContext ["+instanceName+"]");

		return classPathapplicationContext;
	}

	/**
	 * Destroys the Spring context
	 */
	@Override
	public void close() {
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

	public <T> T createBeanAutowireByName(Class<T> beanClass) {
		return SpringUtils.createBean(applicationContext, beanClass);
	}

	/**
	 * Returns the Spring XML Bean Factory If non exists yet it will create one.
	 * If initializing the context fails, it will return null
	 * 
	 * @return Spring XML Bean Factory or NULL
	 */
	protected AbstractApplicationContext getApplicationContext() {
		if(applicationContext == null)
			createApplicationContext();

		return applicationContext;
	}

	public BootState getBootState() {
		return state;
	}

	public Exception getStartupException() {
		if(BootState.ERROR.equals(state)) {
			return startupException;
		}
		return null;
	}

	/**
	 * Register all IBIS modules that can be found on the classpath
	 * TODO: retrieve this (automatically/) through Spring
	 */
	private void lookupApplicationModules() {
		if(!iafModules.isEmpty()) {
			return;
		}

		List<String> modulesToScanFor = new ArrayList<>();
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
			try (InputStream is = pomProperties.openStream()) {
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
