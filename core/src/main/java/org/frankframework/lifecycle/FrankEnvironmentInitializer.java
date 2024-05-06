/*
   Copyright 2019 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.lifecycle;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;

import org.apache.cxf.bus.spring.SpringBus;
import org.apache.logging.log4j.Logger;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

import lombok.extern.log4j.Log4j2;

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
@Order(Ordered.HIGHEST_PRECEDENCE+1) //2nd highest precedence
public class FrankEnvironmentInitializer implements WebApplicationInitializer {
	private static final Logger APPLICATION_LOG = LogUtil.getLogger("APPLICATION");

	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		WebApplicationContext applicationContext = createApplicationContext();
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

	private WebApplicationContext createApplicationContext() {
		APPLICATION_LOG.debug("Starting Frank EnvironmentContext");

		XmlWebApplicationContext applicationContext = new XmlWebApplicationContext();
		applicationContext.setConfigLocations(SpringContextScope.ENVIRONMENT.getContextFile());
		applicationContext.setDisplayName("EnvironmentContext");

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource(SpringContextScope.ENVIRONMENT.getFriendlyName(), AppConstants.getInstance()));

		return applicationContext;
	}
}
