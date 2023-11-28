/*
   Copyright 2019 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ResourceUtils;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import lombok.extern.log4j.Log4j2;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Programmatically load the Frank!Framework Web Environment.
 * It's important this is loaded first, and before any programmatic listeners have been added.
 * The EnvironmentContext will load servlets and filters.
 * 
 * Uses the same order as well as delegation as the SpringBootServletInitializer used in the Frank!Console WAR.
 * 
 * @author Niels Meijer
 */
@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FrankEnvironmentInitializer implements WebApplicationInitializer {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		WebApplicationContext applicationContext = createApplicationContext(servletContext);
		ContextLoader contextLoader = new ContextLoader(applicationContext);

		try {
			WebApplicationContext wac = contextLoader.initWebApplicationContext(servletContext);
			SpringBus bus = (SpringBus) wac.getBean("cxf");
			log.info("Successfully started Frank EnvironmentContext with SpringBus [{}]", bus::getId);
			APPLICATION_LOG.info("Successfully started Frank EnvironmentContext");
		} catch (Exception e) {
			log.fatal("IBIS ApplicationInitializer failed to initialize", e);
			APPLICATION_LOG.fatal("IBIS ApplicationInitializer failed to initialize", e);
			throw e;
		}

		ContextCloseEventListener listener = new ContextCloseEventListener(contextLoader);
		servletContext.addListener(listener);
	}

	private static class ContextCloseEventListener implements ServletContextListener {
		private final ContextLoader contextLoader;

		public ContextCloseEventListener(ContextLoader contextLoader) {
			this.contextLoader = contextLoader;
		}

		@Override
		public void contextInitialized(ServletContextEvent sce) {
			// We don't need to initialize anything, just listen to the close event.
		}

		@Override
		public void contextDestroyed(ServletContextEvent sce) {
			APPLICATION_LOG.info("Stopping Frank EnvironmentContext");
			contextLoader.closeWebApplicationContext(sce.getServletContext());
		}
	}

	private WebApplicationContext createApplicationContext(ServletContext servletContext) {
		System.setProperty(EndpointImpl.CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY_WITH_SECURITY_MANAGER, "false");
		APPLICATION_LOG.debug("Starting Frank EnvironmentContext");

		determineApplicationServerType(servletContext);

		XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();
		applicationContext.setConfigLocations(getSpringConfigurationFiles());
		applicationContext.setDisplayName("EnvironmentContext");

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource(SpringContextScope.ENVIRONMENT.getFriendlyName(), AppConstants.getInstance()));

		return applicationContext;
	}

	private String[] getSpringConfigurationFiles() {
		List<String> springConfigurationFiles = new ArrayList<>();
		springConfigurationFiles.add(SpringContextScope.ENVIRONMENT.getContextFile());

		String file = AppConstants.getInstance().getProperty("ibistesttool.springConfigFile");
		ClassLoader classLoader = this.getClass().getClassLoader();
		URL fileURL = classLoader.getResource(file);
		if(fileURL == null) {
			log.warn("unable to locate TestTool configuration [{}] using classloader [{}]", file, classLoader);
		} else {
			if(file.indexOf(":") == -1) {
				file = ResourceUtils.CLASSPATH_URL_PREFIX+file;
			}

			log.info("loading TestTool configuration [{}]", file);
			springConfigurationFiles.add(file);
		}

		return springConfigurationFiles.toArray(new String[springConfigurationFiles.size()]);
	}

	private void determineApplicationServerType(ServletContext servletContext) {
		String serverInfo = servletContext.getServerInfo();
		String autoDeterminedApplicationServerType = null;
		if (StringUtils.containsIgnoreCase(serverInfo, "Tomcat")) {
			autoDeterminedApplicationServerType = "TOMCAT";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "JBoss")) {
			autoDeterminedApplicationServerType = "JBOSS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "WildFly")) {
			autoDeterminedApplicationServerType = "JBOSS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "jetty")) {
			String javaHome = System.getProperty("java.home");
			if (StringUtils.containsIgnoreCase(javaHome, "tibco")) {
				autoDeterminedApplicationServerType = "TIBCOAMX";
			} else {
				autoDeterminedApplicationServerType = "JETTYMVN";
			}
		} else {
			autoDeterminedApplicationServerType = "TOMCAT";
			APPLICATION_LOG.warn("Unknown server info [{}] default application server type could not be determined, TOMCAT will be used as default value", serverInfo);
		}

		//has it explicitly been set? if not, set the property
		String serverType = System.getProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);
		String serverCustomization = System.getProperty(AppConstants.APPLICATION_SERVER_CUSTOMIZATION_PROPERTY,"");
		if (autoDeterminedApplicationServerType.equals(serverType)) { //and is it the same as the automatically detected version?
			log.info("property [{}] already has a default value [{}]", AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, autoDeterminedApplicationServerType);
		}
		else if (StringUtils.isEmpty(serverType)) { //or has it not been set?
			APPLICATION_LOG.info("Determined ApplicationServer [{}]{}", autoDeterminedApplicationServerType, (StringUtils.isNotEmpty(serverCustomization) ? " customization ["+serverCustomization+"]":""));
			System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, autoDeterminedApplicationServerType);
		}
	}
}
