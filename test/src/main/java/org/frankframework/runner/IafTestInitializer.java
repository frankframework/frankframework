/*
   Copyright 2024-2025 WeAreFrank!

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
package org.frankframework.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
import org.apache.tomcat.websocket.server.WsContextListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import org.frankframework.console.runner.ConsoleWarInitializer;
import org.frankframework.ladybug.runner.LadybugWarInitializer;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.lifecycle.SpringContextScope;
import org.frankframework.lifecycle.servlets.ApplicationServerConfigurer;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
// Careful.. don't log here!!
public class IafTestInitializer {

	public static class ApplicationInitializerWrapper implements ServletContextInitializer {
		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			FrankApplicationInitializer init = new FrankApplicationInitializer();
			init.onStartup(servletContext);
			LogManager.getLogger("APPLICATION").info("Started Frank!Application");
		}
	}

	public static class LadybugInitializerWrapper implements ServletContextInitializer {
		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			System.setProperty("ladybug.jdbc.datasource", "");
			LadybugWarInitializer init = new LadybugWarInitializer();
			init.onStartup(servletContext);
			LogManager.getLogger("APPLICATION").info("Started Ladybug");
		}
	}

	public static class ConsoleInitializerWrapper implements ServletContextInitializer {
		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			ConsoleWarInitializer init = new ConsoleWarInitializer();
			init.onStartup(servletContext);
			LogManager.getLogger("APPLICATION").info("Started Console");
		}
	}

	/** Required to enable the use of WebSockets when starting as (Spring)Boot-able application. */
	public static class WsSciWrapper implements ServletContextInitializer {

		@Override
		public void onStartup(ServletContext servletContext) {
			WsContextListener sc = new WsContextListener();
			sc.contextInitialized(new ServletContextEvent(servletContext));
		}
	}

	public static class ConfigureAppConstants implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource(SpringContextScope.ENVIRONMENT.getFriendlyName(), AppConstants.getInstance()));
		}
	}

	public static void main(String[] args) throws IOException {
		SpringApplication app = configureApplication();
		app.run(args);
	}

	/**
	 * Configure the Frank!Framework application, with options.
	 *
	 * @param appServerCustom Customization option for the AppServer, as used by standard in the framework. Supported in IAF-Test: {@code null}, or {@code "NARAYANA"}.
	 * @param dbms Name of DBMS provider. if {@code null}, then {@code H2} is used as default
	 * @param jmsProvider Name of JMS provider. If null, JMS is not enabled in tests (property {@code jms.active=false}). The JMS provider name is used to set the properties
	 *                    {@code jms.provider.default=<jmsProvider>}, {@code jms.connectionfactory.qcf.<jmsProvider>=jms/qcf-<jmsProvider>} and {@code jms.destination.suffix=-<jmsProvider>}.
	 */
	static SpringApplication configureApplication(@Nullable String appServerCustom, @Nullable String dbms, @Nullable String jmsProvider) throws IOException {
		if (jmsProvider != null) {
			System.setProperty("jms.provider.default", jmsProvider);
		}
		if (appServerCustom != null) {
			System.setProperty(ApplicationServerConfigurer.APPLICATION_SERVER_CUSTOMIZATION_PROPERTY, appServerCustom);
		}
		if (dbms != null) {
			System.setProperty("jdbc.dbms.default", dbms);
		}

		return configureApplication();
	}

	/**
	 * Configure the Frank!Framework application, enabling support for JMS depending on the value of {@literal "jms.provider.default"} System property, with
	 * application server type {@literal "IBISTEST"}.
	 */
	static SpringApplication configureApplication() throws IOException {
		Path projectDir = getProjectDir();

		// Ensure a log.dir has been set
		if (System.getProperty("log.dir") == null) {
			String foundLogDir = getLogDir(projectDir);
			System.setProperty("log.dir", foundLogDir);
			LogUtil.getLogger("APPLICATION").info("set log.dir to [{}]", foundLogDir);
		}

		// Find and configure the configurations
		LogUtil.getLogger("APPLICATION").info("using project.dir [{}]", projectDir);
		setConfigurationsDirectory(projectDir);

		System.setProperty("application.security.http.authentication", "false");
		System.setProperty("application.security.http.transportGuarantee", "none");
		System.setProperty("dtap.stage", "LOC");

		// Configure a CredentialProvider
		System.setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory");
		Path secrets = projectDir.resolve("src/main/secrets/credentials.properties").toAbsolutePath();
		System.setProperty("credentialFactory.map.properties", secrets.toString().replace("\\", "/"));
		System.setProperty("authAliases.expansion.allowed", "testalias");

		// Configure application server type
		System.setProperty(ApplicationServerConfigurer.APPLICATION_SERVER_TYPE_PROPERTY, "IBISTEST");

		// Configure JMS
		String jmsProvider = System.getProperty("jms.provider.default");
		// ServerType "IBISTEST" disables JMS by default, so we need to override it depending on if this property has been set.
		System.setProperty("active.jms", jmsProvider != null ? "true" : "false");
		if (jmsProvider != null) {
			// Setting these properties manually is required with application-server type "IBISTEST"
			System.setProperty("jms.connectionfactory.qcf." + jmsProvider, "jms/qcf-" + jmsProvider);
			System.setProperty("jms.destination.suffix", "-" + jmsProvider);
		}

		String dbms = System.getProperty("jdbc.dbms.default");
		if (dbms != null && !"h2".equals(dbms)) {
			System.setProperty("active.storedProcedureTests", "true");
		}

		// Start the actual application
		SpringApplication app = new SpringApplication();
		app.addInitializers(new ConfigureAppConstants());
		app.setWebApplicationType(WebApplicationType.SERVLET);
		Set<String> set = new HashSet<>();
		app.addPrimarySources(List.of(LadybugInitializerWrapper.class, ApplicationInitializerWrapper.class, ConsoleInitializerWrapper.class, WsSciWrapper.class));
		set.add(SpringContextScope.ENVIRONMENT.getContextFile());
		set.add("TestFrankContext.xml");
		app.setSources(set);

		return app;
	}

	/**
	 * Find the iaf-test module directory
	 * NOTE: Since we still need to determine the log.dir, we may not log anything here!
	 */
	@Nonnull
	public static Path getProjectDir() throws IOException {
		Path runFromDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		return validateIfEclipseOrIntelliJ(runFromDir);
	}

	private static void setConfigurationsDirectory(Path projectDir) throws IOException {
		Path configurationDir = projectDir.resolve("src/main/configurations").toAbsolutePath();
		System.setProperty("configurations.directory", configurationDir.toString());

		// Loop though all directories (depth = 1) + skip current directory.
		try(Stream<Path> folders = Files.walk(configurationDir, 1).skip(1).filter(Files::isDirectory)) {
			folders.forEach(path -> {
				String name = path.getFileName().toString();
				System.setProperty("configurations."+name+".classLoaderType", "ScanningDirectoryClassLoader");
				System.setProperty("configurations."+name+".configurationFile", "Configuration.xml");
				System.setProperty("configurations."+name+".basePath", name);
			});
		}
	}

	/**
	 * Eclipse runs from the module (relative) directory.
	 * IntelliJ runs from the project root directory.
	 * NOTE: Since we still need to determine the log.dir, we may not log anything here!
	 */
	private static @Nonnull Path validateIfEclipseOrIntelliJ(Path runFromDir) throws IOException {
		if(Files.exists(runFromDir.resolve(".github"))) { // this folder exists in the project ROOT directory
			Path module = runFromDir.resolve("test");
			if(Files.exists(module)) {
				return module;
			}
			throw new IOException("assuming we're using IntelliJ but cannot find the FF! Test module");
		}

		return runFromDir;
	}

	// Store the logs by default in ./test/target/logs
	// NOTE: Since we still need to determine the log.dir, we may not log anything here!
	public static String getLogDir(Path projectPath) throws IOException {
		Path targetPath = projectPath.resolve("target");
		if(Files.exists(targetPath) && Files.isDirectory(targetPath)) {
			Path logDir = targetPath.resolve("logs");
			if(!Files.exists(logDir)) {
				Files.createDirectory(logDir);
			}
			return logDir.toAbsolutePath().toString().replace("\\", "/"); // Slashes are required for the larva tool...
		}
		throw new IOException("unable to determine log directory");
	}
}
