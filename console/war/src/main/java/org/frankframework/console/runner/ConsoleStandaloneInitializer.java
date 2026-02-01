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

import java.util.List;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;

import org.apache.tomcat.websocket.server.WsContextListener;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.credentialprovider.CredentialFactory;
import org.frankframework.util.PropertyLoader;

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
public class ConsoleStandaloneInitializer {

	/** Required to enable the use of WebSockets when starting as (Spring)Boot-able application. */
	public static class WsSciWrapper implements ServletContextInitializer {

		@Override
		public void onStartup(@NonNull ServletContext servletContext) {
			WsContextListener sc = new WsContextListener();
			sc.contextInitialized(new ServletContextEvent(servletContext));
		}
	}

	// Should start a XmlServletWebServerApplicationContext.
	// Optionally, in order to enable the ladybug the profile ladybug-file or ladybug-database can be enabled.
	public static void main(String[] args) {
		SpringApplication app = configureApplication();
		app.run(args);
	}

	/**
	 * Configure the Frank!Framework application
	 */
	static SpringApplication configureApplication() {
		SpringApplication app = new SpringApplication();
		app.setAllowBeanDefinitionOverriding(true);
		app.setWebApplicationType(WebApplicationType.SERVLET);
		app.addListeners(new FailedInitializationMonitor());
		app.setSources(Set.of("SpringBootContext.xml"));
		app.addPrimarySources(List.of(WsSciWrapper.class));

		// Custom ClassLoader to ensure we can read from the classpath as well as the far-jar.
		ClassLoader newClassLoader = new DirectoryClassLoader(ClassUtils.getDefaultClassLoader(), ".");
		// I've attempted to set the default ResourceLoader but that breaks the OpenApi configuration.
		// By changing the ResourceLoader, which is not the 'default' WebApplicationContext,
		// it mucks up the OnWebApplicationCondition which has a strange explicit check on the ResourceLoader and not the Context itself.
		app.addInitializers(context -> context.setClassLoader(newClassLoader));
		app.setEnvironment(new PropertyLoaderEnvironment(newClassLoader));

		loadCredentialProvider(newClassLoader);

		return app;
	}

	/** Ugly hack to ensure the correct ClassLoader is used to create the CredentialProvider instance. */
	private static void loadCredentialProvider(ClassLoader newClassLoader) {
		ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			Thread.currentThread().setContextClassLoader(newClassLoader);
			CredentialFactory.getInstance();
		} finally {
			Thread.currentThread().setContextClassLoader(originalClassLoader);
		}
	}

	/**
	 * Custom Environment that uses our own {@link PropertyLoader} to use AdditionalStringResolver.
	 * Also attempts to load properties from the same directory as the WAR file when running as an executable WAR.
	 */
	public static class PropertyLoaderEnvironment extends StandardEnvironment {
		public PropertyLoaderEnvironment(ClassLoader classLoader) {
			super(createPropertySources(classLoader));
		}

		private static MutablePropertySources createPropertySources(ClassLoader classLoader) {
			MutablePropertySources propertySources = new MutablePropertySources();
			propertySources.addLast(new PropertiesPropertySource("application", new PropertyLoader(classLoader, "application.properties")));
			propertySources.addLast(new PropertiesPropertySource("ladybug", new PropertyLoader(classLoader, "testtool.properties")));
			return propertySources;
		}

		@Override
		protected void customizePropertySources(MutablePropertySources propertySources) {
			// NO OP
		}
	}

	/**
	 * When the application fails to start up, trigger a shutdown.
	 * If this is not done the SpringBoot Tomcat instance will continue to work without any application deployed to it.
	 */
	@Log4j2
	private static class FailedInitializationMonitor implements ApplicationListener<ApplicationFailedEvent> {

		@Override
		public void onApplicationEvent(ApplicationFailedEvent event) {
			if (event.getException() != null) {
				log.fatal("unable to start application", event.getException());
			}

			System.exit(1); // Terminate the JVM
		}

	}
}
