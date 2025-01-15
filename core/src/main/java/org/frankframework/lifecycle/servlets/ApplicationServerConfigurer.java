/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.lifecycle.servlets;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.WebApplicationInitializer;

import lombok.extern.log4j.Log4j2;

/**
 * It's important this is loaded first, and before any programmatic listeners have been added to determine the Application Server type.
 * We should technically use different profiles for this, but for now, it overwites the default Spring Context
 *
 * @author Niels Meijer
 */
@Log4j2
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApplicationServerConfigurer implements WebApplicationInitializer {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");

	// Statics are defined here before independent of AppConstants, so that class does not need to be loaded yet.
	public static final String APPLICATION_SERVER_TYPE_PROPERTY = "application.server.type";
	public static final String APPLICATION_SERVER_CUSTOMIZATION_PROPERTY = "application.server.type.custom";

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		System.setProperty(EndpointImpl.CHECK_PUBLISH_ENDPOINT_PERMISSON_PROPERTY_WITH_SECURITY_MANAGER, "false");

		String serverInfo = servletContext.getServerInfo();
		String autoDeterminedApplicationServerType = null;
		if (StringUtils.containsIgnoreCase(serverInfo, "Tomcat")) {
			autoDeterminedApplicationServerType = "TOMCAT";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "JBoss")) {
			autoDeterminedApplicationServerType = "JBOSS";
		} else if (StringUtils.containsIgnoreCase(serverInfo, "WildFly")) {
			autoDeterminedApplicationServerType = "JBOSS";
		} else {
			autoDeterminedApplicationServerType = "TOMCAT";
			APPLICATION_LOG.warn("Unknown server info [{}] default application server type could not be determined, TOMCAT will be used as default value", serverInfo);
		}

		// has it explicitly been set? if not, set the property
		String serverType = System.getProperty(APPLICATION_SERVER_TYPE_PROPERTY);
		String serverCustomization = System.getProperty(APPLICATION_SERVER_CUSTOMIZATION_PROPERTY,"");
		if (autoDeterminedApplicationServerType.equals(serverType)) { // and is it the same as the automatically detected version?
			log.info("property [{}] already has a default value [{}]", APPLICATION_SERVER_TYPE_PROPERTY, autoDeterminedApplicationServerType);
		} else if (StringUtils.isEmpty(serverType)) { // or has it not been set?
			APPLICATION_LOG.info("Determined ApplicationServer [{}]{}", autoDeterminedApplicationServerType, (StringUtils.isNotEmpty(serverCustomization) ? " customization ["+serverCustomization+"]":""));
			System.setProperty(APPLICATION_SERVER_TYPE_PROPERTY, autoDeterminedApplicationServerType);
		}
	}
}
