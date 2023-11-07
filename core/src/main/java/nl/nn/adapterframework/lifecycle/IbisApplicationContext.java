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

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import nl.nn.adapterframework.util.Environment;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * Creates and maintains the (Spring) Application Context. If the context is loaded through a {@link IbisApplicationServlet servlet}
 * it will register the servlet in the context. When the Application Context is created or destroyed it will also create/destroy the servlet.
 * This ensures that the correct {@link SpringBus bus} will be used in which CXF will register it's endpoints and dispatchers.
 * <p>
 * <br/><br/>
 * <p>
 * It is important that the Application Context is created before the {@link IbisApplicationServlet servlet} initializes.
 * Otherwise the servlet will register under the wrong {@link SpringBus bus}!
 * <p>
 * <br/><br/>
 * <p>
 * It is possible to retrieve the Application Context through the Spring WebApplicationContextUtils class
 *
 * @see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
 */
public class IbisApplicationContext implements Closeable {
	private Exception startupException;

	public enum BootState {
		FIRST_START, STARTING, STARTED, STOPPING, STOPPED, ERROR;
	}

	private AbstractApplicationContext applicationContext;
	private ApplicationContext parentContext = null;

	protected static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private static final Logger LOG = LogUtil.getLogger(IbisApplicationContext.class);
	private final Logger applicationLog = LogUtil.getLogger("APPLICATION");
	private BootState state = BootState.FIRST_START;
	private final Map<String, String> iafModules = new HashMap<>();


	public void setParentContext(ApplicationContext parentContext) {
		this.parentContext = parentContext;
	}

	/**
	 * Create Spring Bean factory.
	 * <p>
	 * Create the Spring Bean Factory using the default <code>springContext</code>,
	 * if not <code>null</code>.
	 *
	 * @throws BeansException If the Factory can not be created.
	 */
	protected void createApplicationContext() throws BeansException {
		applicationLog.debug("Creating IbisApplicationContext");
		if (!state.equals(BootState.FIRST_START)) {
			state = BootState.STARTING;
		}
		if (startupException != null) {
			startupException = null;
		}

		long start = System.currentTimeMillis();

		lookupApplicationModules();

		try {
			applicationContext = createClassPathApplicationContext();
			if (parentContext != null) {
				LOG.info("found Spring rootContext [{}]", parentContext);
				applicationContext.setParent(parentContext);
			}
			applicationContext.refresh();
		} catch (BeansException be) {
			state = BootState.ERROR;
			startupException = be; //Save this in case we might need it later
			throw be;
		}

		applicationLog.info("Created IbisApplicationContext [{}] in {} ms", applicationContext::getId, () -> (System.currentTimeMillis() - start));
		state = BootState.STARTED;
	}

	/**
	 * Loads springUnmanagedDeployment, SpringApplicationContext and files specified by the SPRING.CONFIG.LOCATIONS
	 * property in AppConstants.properties
	 *
	 * @param classLoader to use in order to find and validate the Spring Configuration files
	 * @return A String array containing all files to use.
	 */
	protected String[] getSpringConfigurationFiles(ClassLoader classLoader) {
		List<String> springConfigurationFiles = new ArrayList<>();
		if (parentContext == null) { //When not running in a web container, populate top-level beans so they can be found throughout this/sub-contexts.
			springConfigurationFiles.add(SpringContextScope.STANDALONE.getContextFile());
		}
		springConfigurationFiles.add(SpringContextScope.APPLICATION.getContextFile());
		String configLocations = AppConstants.getInstance().getProperty("SPRING.CONFIG.LOCATIONS");
		springConfigurationFiles.addAll(splitIntoConfigFiles(classLoader, configLocations));
		addJmxConfigurationIfEnabled(springConfigurationFiles);

		LOG.info("loading Spring configuration files {}", springConfigurationFiles);
		return springConfigurationFiles.toArray(new String[springConfigurationFiles.size()]);
	}

	private List<String> splitIntoConfigFiles(ClassLoader classLoader, String fileList) {
		return Arrays
			.stream(fileList.split(","))
			.filter(filename -> isSpringConfigFileOnClasspath(classLoader, filename))
			.map(this::addClasspathPrefix)
			.collect(Collectors.toList());
	}

	private boolean isSpringConfigFileOnClasspath(ClassLoader classLoader, String filename) {
		URL fileURL = classLoader.getResource(filename);
		if (fileURL == null) {
			LOG.error("unable to locate Spring configuration file [{}]", filename);
		}
		return fileURL != null;
	}

	private String addClasspathPrefix(String filename) {
		if (filename.contains(":")) {
			return filename;
		}
		return ResourceUtils.CLASSPATH_URL_PREFIX + "/" + filename;
	}

	private void addJmxConfigurationIfEnabled(List<String> springConfigurationFiles) {
		boolean jmxEnabled = AppConstants.getInstance().getBoolean("management.endpoints.jmx.enabled", false);
		if (jmxEnabled) {
			springConfigurationFiles.add(ResourceUtils.CLASSPATH_URL_PREFIX + "/" + "SpringApplicationContextJMX.xml");
		}
	}

	/**
	 * Creates the Spring Application Context when ran from the command line.
	 *
	 * @throws BeansException when the Context fails to initialize
	 */
	private ClassPathXmlApplicationContext createClassPathApplicationContext() {
		ClassPathXmlApplicationContext classPathApplicationContext = new ClassPathXmlApplicationContext();

		MutablePropertySources propertySources = classPathApplicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource(SpringContextScope.APPLICATION.getFriendlyName(), APP_CONSTANTS));

		ClassLoader classLoader = classPathApplicationContext.getClassLoader();
		if (classLoader == null) throw new IllegalStateException("no ClassLoader found to initialize Spring from");
		classPathApplicationContext.setConfigLocations(getSpringConfigurationFiles(classLoader));

		String instanceName = APP_CONSTANTS.getProperty("instance.name");
		classPathApplicationContext.setId(requireNonNull(instanceName));
		classPathApplicationContext.setDisplayName("IbisApplicationContext [" + instanceName + "]");

		return classPathApplicationContext;
	}

	/**
	 * Destroys the Spring context
	 */
	@Override
	public void close() {
		if (applicationContext != null) {
			String oldContextName = applicationContext.getId();
			LOG.info("closing IbisApplicationContext [{}]", oldContextName);

			applicationContext.close();
			applicationContext = null;

			applicationLog.info("Closed IbisApplicationContext [{}]", oldContextName);
		}
	}

	@Deprecated
	public <T> T getBean(String beanName, Class<T> beanClass) {
		return applicationContext.getBean(beanName, beanClass);
	}

	@Deprecated
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
		if (applicationContext == null)
			createApplicationContext();

		return applicationContext;
	}

	public BootState getBootState() {
		return state;
	}

	public Exception getStartupException() {
		if (BootState.ERROR.equals(state)) {
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
		modulesToScanFor.add("ibis-adapterframework-aspose");
		modulesToScanFor.add("ibis-adapterframework-aws");
		modulesToScanFor.add("ibis-adapterframework-cmis");
		modulesToScanFor.add("ibis-adapterframework-commons");
		modulesToScanFor.add("ibis-adapterframework-console-backend");
		modulesToScanFor.add("ibis-adapterframework-coolgen");
		modulesToScanFor.add("ibis-adapterframework-core");
		modulesToScanFor.add("credentialprovider");
		modulesToScanFor.add("ibis-adapterframework-ibm");
		modulesToScanFor.add("ibis-adapterframework-idin");
		modulesToScanFor.add("ibis-adapterframework-ifsa");
		modulesToScanFor.add("ibis-adapterframework-ladybug");
		modulesToScanFor.add("ibis-adapterframework-larva");
		modulesToScanFor.add("iaf-management-gateway");
		modulesToScanFor.add("ibis-adapterframework-sap");
		modulesToScanFor.add("ibis-adapterframework-tibco");
		modulesToScanFor.add("ibis-adapterframework-webapp");

		registerApplicationModules(modulesToScanFor);
	}

	/**
	 * Register IBIS modules that can be found on the classpath
	 *
	 * @param modules list with modules to register
	 */
	private void registerApplicationModules(List<String> modules) {
		for (String module : modules) {
			String version = Environment.getModuleVersion(module);

			if (version != null) {
				iafModules.put(module, version);
				APP_CONSTANTS.put(module + ".version", version);
				applicationLog.debug("Loading IAF module [{}] version [{}]", module, version);
			}
		}
	}
}
