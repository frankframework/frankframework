/*
   Copyright 2023-2026 WeAreFrank!

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
package org.frankframework.console.runner;

import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.springframework.beans.BeansException;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.context.WebApplicationContext;

import lombok.Setter;
import lombok.extern.log4j.Log4j2;

import org.frankframework.lifecycle.servlets.SecuritySettings;

/**
 * Spring Boot entrypoint when running as a normal WAR application.
 *
 * @author Niels Meijer
 */
@Order(400)
public class ConsoleWarInitializer extends SpringBootServletInitializer {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");

	@Configuration
	public static class WarConfiguration implements ApplicationContextAware {
		// NO OP required for Spring Boot. Used when running an Annotation Based config, which we are not, see setSources(...) in run(SpringApplication).

		private @Setter ApplicationContext applicationContext;

		/**
		 * Apparently this is required, though I'm unsure why.
		 *
		 * Prevents `Failed to register 'filter springSecurityFilterChain' on the servlet context. Possibly already registered?`.
		 * Does not need to be disabled `bean.setEnabled(false)`. Just having the bean here is enough.
		 */
		@Bean
		public FilterRegistrationBean<Filter> getFilterRegistrationBean() {
			FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>();
			Filter filter = applicationContext.getBean(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME, Filter.class);
			bean.setFilter(filter);
			return bean;
		}

	}

	// Purely here for some debug info
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException {
		APPLICATION_LOG.debug("Starting Frank!Framework Console");
		final long start = System.currentTimeMillis();

		disableSpringBootLogging();

		try {
			super.onStartup(servletContext);
			APPLICATION_LOG.fatal("Started Frank!Framework Console in {} ms", () -> (System.currentTimeMillis() - start));
		} catch (Exception e) {
			APPLICATION_LOG.fatal("Unable to start Frank!Framework Console", e);
			throw e;
		}
	}

	/**
	 * Disable the Spring-Boot LoggingSystem.
	 * Spring programmatically adds Console appenders and configures it's format regardless of what we configure.
	 * 
	 * See {@link Log4J2LoggingSystem#initialize(org.springframework.boot.logging.LoggingInitializationContext, String, org.springframework.boot.logging.LogFile)}.
	 */
	private void disableSpringBootLogging() {
		LoggerContext logContext = LoggerContext.getContext(false);
		logContext.setExternalContext(LoggingSystem.class.getName());
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		builder.sources(WarConfiguration.class);
		setRegisterErrorPageFilter(false);
		builder.bannerMode(Mode.OFF);
		return super.configure(builder);
	}

	@Override
	protected WebApplicationContext run(SpringApplication application) {
		Set<String> set = new HashSet<>();
		set.add("FrankConsoleContext.xml");
		application.setWebApplicationType(WebApplicationType.NONE);

		// Not ideal, but allows us to delegate property resolving to the parent context if present.
		// When running the console as a standalone jar, this code wont be used.
		StandardEnvironment environment = new StandardEnvironment();
		MutablePropertySources propertySources = environment.getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		application.setEnvironment(environment);
		application.setSources(set);
		application.addListeners(new FailedInitializationMonitor());

		// This will be exposed on the <context-path>/api/openapi/v3 url
		System.setProperty("springdoc.api-docs.path", "/openapi/v3");

		// When running the console in a traditional WAR environment this needs to be called.
		SecuritySettings.setupDefaultSecuritySettings(environment);

		return super.run(application);
	}

	/**
	 * When the application fails to start up, trigger a shutdown and log the exception.
	 */
	@Log4j2
	private static class FailedInitializationMonitor implements ApplicationListener<ApplicationFailedEvent> {

		@Override
		public void onApplicationEvent(ApplicationFailedEvent event) {
			if (event.getException() != null) {
				log.fatal("unable to start application", event.getException());
				APPLICATION_LOG.fatal("unable to start application: {}", () -> getRootCause(event.getException()).getMessage());
			}

			System.exit(1); // Terminate the JVM
		}

		private Throwable getRootCause(Throwable t) {
			if (t instanceof BeansException || t instanceof ApplicationContextException) {
				return (t.getCause() != null) ? getRootCause(t.getCause()) : t;
			}
			return t;
		}

	}
}
