/*
   Copyright 2019 Nationale-Nederlanden

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
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import nl.nn.adapterframework.extensions.cxf.NamespaceUriProviderManager;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.WebApplicationContext;
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
	enum BootState {
		FIRST_START,STARTING,STARTED,STOPPING,STOPPED;
	}

	private AbstractApplicationContext applicationContext;
	private IbisApplicationServlet servlet = null;
	public final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private Logger log = LogUtil.getLogger(this);
	private final String SPRINGCONTEXT = "/springContext.xml";
	private ServletManager servletManager = null;
	private BootState STATE = BootState.FIRST_START;

	private NamespaceUriProviderManager namespaceUriProviderManager = new NamespaceUriProviderManager();

	public void setServletConfig(IbisApplicationServlet servletConfig) {
		this.servlet = servletConfig;
		servletManager = new ServletManager(servletConfig);
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
	public void createApplicationContext() throws BeansException {
		log.debug("creating Spring Application Context");
		if(!STATE.equals(BootState.FIRST_START))
			STATE = BootState.STARTING;

		long start = System.currentTimeMillis();

		if(servlet == null) {
			log.debug("no servlet found, using ClassPathApplicationContext");
			applicationContext = createClassPathApplicationContext();
		}
		else {
			log.debug("found servletconfig, using WebApplicationContext");
			applicationContext = createWebApplicationContext();

			//When the IBIS is started from a servlet, start the NamespaceUriProviderManager (servlet/rpcrouter)
			namespaceUriProviderManager.init();
		}

		registerApplicationModules();

		log.info("created "+applicationContext.getClass().getSimpleName()+" in " + (System.currentTimeMillis() - start) + " ms");
		STATE = BootState.STARTED;
	}

	/**
	 * Creates the Spring Application Context when ran from a {@link IbisApplicationServlet servlet}
	 * @throws BeansException when the Context fails to initialize
	 */
	private XmlWebApplicationContext createWebApplicationContext() throws BeansException {
		String springContext = XmlWebApplicationContext.CLASSPATH_URL_PREFIX + SPRINGCONTEXT;
		XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource("ibis", APP_CONSTANTS));

		applicationContext.setConfigLocation(springContext);
		applicationContext.setServletConfig(servlet.getServletConfig());

		applicationContext.refresh();

		//Look for IbisInitializer annotations, parse them as beans and 'autowire' *-aware interfaces
		loadIbisInitializers(applicationContext);

		//Makes it possible to retrieve the Application Context through the Spring WebApplicationContextUtils class (e.q. cxf)
		//@see org.springframework.web.context.support.WebApplicationContextUtils#getWebApplicationContext
		servlet.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, applicationContext);

		//Retrieve and set the newly created SpringBus as default bus to use
		Bus bus = (Bus) applicationContext.getBean("cxf");
		if(bus instanceof SpringBus) {
			log.debug("setting default CXF SpringBus["+bus.getId()+"]");
			BusFactory.setDefaultBus(bus);
		}

		//Initialize the servlet
		try {
			servlet.doInit();
		} catch (ServletException e) {
			throw new RuntimeException("unable to run #CXFServlet.init();");
		}

		return applicationContext;
	}

	private void loadIbisInitializers(AbstractApplicationContext applicationContext) {
		BeanDefinitionRegistry factory = (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
		try {
			Map<String, Object> interfaces = applicationContext.getBeansWithAnnotation(IbisInitializer.class);

			for (String beanName : interfaces.keySet()) {
				Object clazz = interfaces.get(beanName);
//				IbisInitializer metaData = clazz.getClass().getAnnotation(IbisInitializer.class);

				if(servlet != null)
					servlet.getServletContext().log("Autowiring IbisInitializer ["+beanName+"]");

				if(clazz != null) {
					if(servlet != null && STATE.equals(BootState.FIRST_START)) {
						if(clazz instanceof DynamicRegistration.Servlet) {
							DynamicRegistration.Servlet servlet = (DynamicRegistration.Servlet) clazz;
							log.info("adding servlet ["+servlet.getName()+"] to context");
							servletManager.register(servlet);
						}

						factory.removeBeanDefinition(beanName);
						log.debug("unwired DynamicRegistration.Servlet ["+beanName+"]");
					}

					log.debug("instantiated IbisInitializer ["+beanName+"]");
				}
			}
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
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
		applicationContext.setConfigLocation(SPRINGCONTEXT);
		applicationContext.refresh();

		return applicationContext;
	}

	/**
	 * Destroys the Spring context
	 */
	protected void destroyApplicationContext() {
		
		namespaceUriProviderManager.destroy();

		if (applicationContext != null) {
			String oldContextName = applicationContext.getDisplayName();
			log.debug("destroying Ibis Application Context ["+oldContextName+"]");

			((ConfigurableApplicationContext)applicationContext).close();

			if(servlet != null)
				servlet.doDestroy();

			applicationContext = null;

			log.info("destroyed Ibis Application Context ["+oldContextName+"]");
		}
	}

	public Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}

	public Object getBean(String beanName, Class<?> beanClass) {
		return applicationContext.getBean(beanName, beanClass);
	}

	public Object createBeanAutowireByName(Class<?> beanClass) {
		return applicationContext.getAutowireCapableBeanFactory().createBean(
				beanClass, AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
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
	

	/**
	 * Register all IBIS modules that can be found on the classpath
	 * TODO: retrieve this (automatically/) through Spring
	 */
	private void registerApplicationModules() {
		List<String> iafModules = new ArrayList<String>();

		iafModules.add("ibis-adapterframework-akamai");
		iafModules.add("ibis-adapterframework-cmis");
		iafModules.add("ibis-adapterframework-coolgen");
		iafModules.add("ibis-adapterframework-core");
		iafModules.add("ibis-adapterframework-ibm");
		iafModules.add("ibis-adapterframework-idin");
		iafModules.add("ibis-adapterframework-ifsa");
		iafModules.add("ibis-adapterframework-ladybug");
		iafModules.add("ibis-adapterframework-larva");
		iafModules.add("ibis-adapterframework-sap");
		iafModules.add("ibis-adapterframework-tibco");
		iafModules.add("ibis-adapterframework-webapp");

		registerApplicationModules(iafModules);
	}

	/**
	 * Register IBIS modules that can be found on the classpath
	 * @param iafModules list with modules to register
	 */
	private void registerApplicationModules(List<String> iafModules) {
		for(String module: iafModules) {
			String version = getModuleVersion(module);

			if(version != null) {
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
