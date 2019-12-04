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

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.WebApplicationInitializer;

/**
 * Interface to be implemented in Servlet 3.0+ environments in order to configure the
 * {@link ServletContext} programmatically -- as opposed to (or possibly in conjunction
 * with) the traditional {@code web.xml}-based approach.
 *
 * <p>Implementations of this SPI will be detected automatically by {@link
 * SpringServletContainerInitializer}, which itself is bootstrapped automatically
 * by any Servlet 3.0 container. See {@linkplain SpringServletContainerInitializer its
 * Javadoc} for details on this bootstrapping mechanism.
 * 
 * 
 * @see "https://docs.oracle.com/javase/tutorial/ext/basics/spi.html"
 * 
 * @author Niels Meijer
 *
 */
public class IbisApplicationInitializer implements WebApplicationInitializer {

	private ServletContext servletContext;
	private Logger log = LogUtil.getLogger(this);

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		servletContext.log("Starting IBIS Application");
		this.servletContext = servletContext;

		checkAndCorrectLegacyServerTypes();
		determineApplicationServerType();

		//TODO start the springContext from here!
	}

	/**
	 * Log the message in System.out, the Ibis console and the Log4j logger
	 * @param message to log
	 */
	private void log(String message) {
		servletContext.log(message);
		ConfigurationWarnings.getInstance().add(log, message);
	}

	private void checkAndCorrectLegacyServerTypes() {
		//In case the property is explicitly set with an unsupported value, E.g. 'applName + number'
		String applicationServerType = System.getProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);
		if (StringUtils.isNotEmpty(applicationServerType)) {
			if (applicationServerType.equalsIgnoreCase("WAS5") || applicationServerType.equalsIgnoreCase("WAS6")) {
				log("interpeting value ["+applicationServerType+"] of property ["+AppConstants.APPLICATION_SERVER_TYPE_PROPERTY+"] as [WAS]");
				System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, "WAS");
			} else if (applicationServerType.equalsIgnoreCase("TOMCAT6")) {
				log("interpeting value ["+applicationServerType+"] of property ["+AppConstants.APPLICATION_SERVER_TYPE_PROPERTY+"] as [TOMCAT]");
				System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, "TOMCAT");
			}
		}
	}

	private void determineApplicationServerType() {
		String serverInfo = servletContext.getServerInfo();
		String defaultApplicationServerType = null;
		if (StringUtils.containsIgnoreCase(serverInfo, "WebSphere Liberty")) {
			defaultApplicationServerType = "WLP";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "WebSphere")) {
			defaultApplicationServerType = "WAS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "Tomcat")) {
			defaultApplicationServerType = "TOMCAT";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "JBoss")) {
			defaultApplicationServerType = "JBOSS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "WildFly")) {
			defaultApplicationServerType = "JBOSS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "jetty")) {
			String javaHome = System.getProperty("java.home");
			if (StringUtils.containsIgnoreCase(javaHome, "tibco")) {
				defaultApplicationServerType = "TIBCOAMX";
			} else {
				defaultApplicationServerType = "JETTYMVN";
			}
		} else {
			defaultApplicationServerType = "TOMCAT";
			log("unknown server info ["+serverInfo+"] default application server type could not be determined, TOMCAT will be used as default value");
		}

		//has it explicitly been set? if not, set the property
		String serverType = System.getProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY);
		if (defaultApplicationServerType.equals(serverType)) { //and is it the same as the automatically detected version?
			log("property ["+AppConstants.APPLICATION_SERVER_TYPE_PROPERTY+"] already has a default value ["+defaultApplicationServerType+"]");
		}
		else if (StringUtils.isEmpty(serverType)) { //or has it not been set?
			System.setProperty(AppConstants.APPLICATION_SERVER_TYPE_PROPERTY, defaultApplicationServerType);
		}
	}
}
