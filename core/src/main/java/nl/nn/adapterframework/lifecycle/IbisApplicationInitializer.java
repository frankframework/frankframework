/*
   Copyright 2019 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.bus.spring.SpringBus;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ResourceUtils;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Starts a Spring Context before all Servlets are initialized. This allows the use of dynamically creating 
 * Spring wired Servlets (using the {@link ServletManager}). These beans can be retrieved later on from within
 * the IbisContext, and are unaffected by the {@link IbisContext#fullReload()}.
 * 
 * @author Niels Meijer
 *
 */
public class IbisApplicationInitializer extends ContextLoaderListener {
	private Logger log = LogUtil.getLogger(this);
	private final Logger applicationLog = LogUtil.getLogger("APPLICATION");

	@Override
	protected WebApplicationContext createWebApplicationContext(ServletContext servletContext) {
		System.setProperty(EndpointImpl.CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY_WITH_SECURITY_MANAGER, "false");
		applicationLog.debug("Starting IBIS WebApplicationInitializer");

		checkAndCorrectLegacyServerTypes();
		determineApplicationServerType(servletContext);

		XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();
		applicationContext.setConfigLocations(getSpringConfigurationFiles());
		applicationContext.setDisplayName("IbisApplicationInitializer");

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

	@Override
	public void closeWebApplicationContext(ServletContext servletContext) {
		applicationLog.info("Stopping IBIS WebApplicationInitializer");
		super.closeWebApplicationContext(servletContext);
	}

	/*
	 * Purely here to print the CXF SpringBus ID
	 */
	@Override
	public WebApplicationContext initWebApplicationContext(ServletContext servletContext) {
		try {
			WebApplicationContext wac = super.initWebApplicationContext(servletContext);
			SpringBus bus = (SpringBus) wac.getBean("cxf");
			log.info("Successfully started IBIS WebApplicationInitializer with SpringBus [{}]", bus::getId);
			applicationLog.info("Successfully started IBIS WebApplicationInitializer");
			return wac;
		} catch (Exception e) {
			log.fatal("IBIS ApplicationInitializer failed to initialize", e);
			applicationLog.fatal("IBIS ApplicationInitializer failed to initialize", e);
			throw e;
		}
	}

	//TODO: remove this in 8.0
	private void checkAndCorrectLegacyServerTypes() {
		//In case the property is explicitly set with an unsupported value, E.g. 'applName + number'
		String applicationServerType = System.getProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);
		if (StringUtils.isNotEmpty(applicationServerType)) {
			if (applicationServerType.equalsIgnoreCase("WAS5") || applicationServerType.equalsIgnoreCase("WAS6")) {
				log.warn("interpeting value ["+applicationServerType+"] of property ["+AppConstants.APPLICATION_SERVER_TYPE_PROPERTY+"] as [WAS]");
				System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, "WAS");
			} else if (applicationServerType.equalsIgnoreCase("TOMCAT6")) {
				log.warn("interpeting value ["+applicationServerType+"] of property ["+AppConstants.APPLICATION_SERVER_TYPE_PROPERTY+"] as [TOMCAT]");
				System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, "TOMCAT");
			}
		}
	}

	private void determineApplicationServerType(ServletContext servletContext) {
		String serverInfo = servletContext.getServerInfo();
		String autoDeterminedApplicationServerType = null;
		if (StringUtils.containsIgnoreCase(serverInfo, "WebSphere Liberty")) {
			autoDeterminedApplicationServerType = "WLP";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "WebSphere")) {
			autoDeterminedApplicationServerType = "WAS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "Tomcat")) {
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
			applicationLog.warn("Unknown server info [{}] default application server type could not be determined, TOMCAT will be used as default value", serverInfo);
		}

		//has it explicitly been set? if not, set the property
		String serverType = System.getProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);
		String serverCustomization = System.getProperty(AppConstants.APPLICATION_SERVER_CUSTOMIZATION_PROPERTY,"");
		if (autoDeterminedApplicationServerType.equals(serverType)) { //and is it the same as the automatically detected version?
			log.info("property ["+AppConstants.APPLICATION_SERVER_TYPE_PROPERTY+"] already has a default value ["+autoDeterminedApplicationServerType+"]");
		}
		else if (StringUtils.isEmpty(serverType)) { //or has it not been set?
			String logLine = "Determined ApplicationServer ["+autoDeterminedApplicationServerType+"]"+(StringUtils.isNotEmpty(serverCustomization)? " customization ["+serverCustomization+"]":"");
			servletContext.log(logLine);
			applicationLog.info(logLine);
			System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, autoDeterminedApplicationServerType);
		}
	}
}
