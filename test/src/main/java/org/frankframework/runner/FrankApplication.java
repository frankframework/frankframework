/*
   Copyright 2024-2026 WeAreFrank!

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

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.tomcat.websocket.server.WsContextListener;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.BeansException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.boot.logging.log4j2.Log4J2LoggingSystem;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.messaging.Message;

import lombok.extern.log4j.Log4j2;

import org.frankframework.console.runner.ConsoleWarInitializer;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.credentialprovider.CredentialAlias;
import org.frankframework.credentialprovider.CredentialFactory;
import org.frankframework.credentialprovider.ISecret;
import org.frankframework.credentialprovider.ISecretProvider;
import org.frankframework.ladybug.runner.LadybugWarInitializer;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.lifecycle.SpringContextScope;
import org.frankframework.lifecycle.servlets.ApplicationServerConfigurer;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.management.bus.LocalGateway;
import org.frankframework.management.bus.message.MessageBase;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.SpringUtils;

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
// Careful.. don't log here!!
public class FrankApplication implements Closeable {
	private final Path projectDir;
	private final SpringApplication app;
	private boolean configuredCredentialProvider = false;

	@Nullable
	private ConfigurableApplicationContext applicationContext;

	/**
	 * Configure the Frank!Framework application, using application server type {@literal "IBISTEST"}.
	 */
	public FrankApplication() throws IOException {
		this(findProjectDir()); // Module root directory
	}
	public FrankApplication(Path projectDir) throws IOException {
		this.projectDir = projectDir;

		// Ensure a log.dir has been set
		if (System.getProperty("log.dir") == null) {
			String foundLogDir = getLogDir(projectDir);
			System.setProperty("log.dir", foundLogDir);
			LogUtil.getLogger("APPLICATION").info("set log.dir to [{}]", foundLogDir);
		}

		disableSpringBootLogging();
		disableAuthentication();

		// Make the console available on the root endpoint.
		System.setProperty("servlet.IAF-GUI.urlMapping", "/*,/iaf/gui/*");

		LogUtil.getLogger("APPLICATION").info("using project.dir [{}]", projectDir);

		// Configure application server type
		System.setProperty(ApplicationServerConfigurer.APPLICATION_SERVER_TYPE_PROPERTY, "IBISTEST");

		// Start the actual application
		app = new SpringApplication();
		app.addInitializers(new ConfigureAppConstants());
		app.setWebApplicationType(WebApplicationType.SERVLET);
		app.addListeners(new FailedInitializationMonitor());

		Set<String> set = new HashSet<>();
		app.addPrimarySources(List.of(LadybugInitializerWrapper.class, ApplicationInitializerWrapper.class, ConsoleInitializerWrapper.class, WsSciWrapper.class));
		set.add(SpringContextScope.ENVIRONMENT.getContextFile());
		set.add("TestFrankContext.xml");
		app.setSources(set);
	}

	// TODO figure out how to deal with the CredentialFactory

	/**
	 * Configure a CredentialProvider
	 * @throws FileNotFoundException If the properties file cannot be found
	 */
	public void configureCredentialProvider(String credentialPropertiesFile) throws IOException {
		// If a CredentialFactory has already been set, abort.
		if (configuredCredentialProvider == true || StringUtils.isNotBlank(System.getProperty("credentialFactory.class"))) {
			// skip!
			return;
		}
		configuredCredentialProvider = true;

		// If no file has been provided, use a NO-OP provider.
		if (StringUtils.isBlank(credentialPropertiesFile)) {
			try {
				CredentialFactory.createInstance(new NoopCredentialFactory());
			} catch (Exception e) {
				LogUtil.getLogger("APPLICATION").fatal("unable to install  NoopCredentialFactory", e);
			}
			return;
		}

		URL url = ClassUtils.getResourceURL(credentialPropertiesFile);
		if (url == null) {
			// So not on the local ClassPath, perhaps absolute or relative?
			Path secrets = getProjectDir().resolve(credentialPropertiesFile).toAbsolutePath();
			try {
				url = secrets.toUri().toURL();
			} catch (MalformedURLException e) {
				throw new IOException("unable to load ["+secrets+"]", e);
			}
		}

		System.setProperty("credentialFactory.class", "org.frankframework.credentialprovider.PropertyFileCredentialFactory");
		System.setProperty("credentialFactory.map.properties", url.toString().replace("\\", "/"));
		CredentialFactory.getInstance();
	}

	public static class NoopCredentialFactory implements ISecretProvider {
		@Override
		public void initialize() throws Exception {
			// Nothing to initialize
		}

		@Override
		public boolean hasSecret(CredentialAlias alias) {
			return false;
		}

		@Override
		public ISecret getSecret(CredentialAlias alias) throws NoSuchElementException {
			throw new NoSuchElementException();
		}

		@Override
		public @Nullable Collection<String> getConfiguredAliases() {
			return null;
		}
	}

	public Path getProjectDir() {
		return projectDir;
	}

	public ConfigurableApplicationContext run(String... args) {
		try {
			configureCredentialProvider(null);
		} catch (IOException e) {
			// Ignored, never happens with a NOOP provider.
		}

		applicationContext = app.run(args);

		TomcatServletWebServerFactory tomcat = applicationContext.getBean("tomcat", TomcatServletWebServerFactory.class);
		String baseUrl = String.format("http://localhost:%d%s/", tomcat.getPort(), tomcat.getContextPath());

		LogUtil.getLogger("APPLICATION").info("Application running on [{}]", baseUrl);
		return applicationContext;
	}

	@Override
	public void close() {
		if (applicationContext != null) {
			SpringApplication.exit(applicationContext);
		}

		System.clearProperty("credentialFactory.class");
		System.clearProperty("credentialFactory.map.properties");
		// Make sure to clear the app constants as well
		AppConstants.removeInstance();
	}

	public boolean isRunning() {
		return applicationContext != null && applicationContext.isRunning();
	}

	public boolean hasStarted() {
		if (applicationContext == null || !isRunning()) {
			return false;
		}

		try {
			LocalGateway gateway = createBean();
			Message<Object> response = gateway.sendSyncMessage(RequestMessageBuilder.create(BusTopic.HEALTH).build(null));
			return "200".equals(response.getHeaders().get(BusMessageUtils.HEADER_PREFIX+MessageBase.STATUS_KEY));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Performs full initialization of the bean, including all applicable BeanPostProcessors. This is effectively a superset of what autowire provides, adding initializeBean behavior.
	 * This method can be used when the compiler can statically determine the class from the variable to which the bean is assigned.
	 * Do not pass actual argument to reified, Java will auto-detect the class of the bean type.
	 */
	@SafeVarargs
	public final <T> T createBean(T... reified) {
		return SpringUtils.createBean(applicationContext, reified);
	}

	/**
	 * Do not pass actual argument to reified, Java will auto-detect the class of the bean type.
	 */
	@SafeVarargs
	public final <T> T getBean(String name, T... reified) {
		return applicationContext.getBean(name, SpringUtils.getClassOf(reified));
	}

	/**
	 * Do not pass actual argument to reified, Java will auto-detect the class of the bean type.
	 */
	@SafeVarargs
	public final <T> T getBean(T... reified) {
		return applicationContext.getBean(SpringUtils.getClassOf(reified));
	}

	/**
	 * Find the iaf-test module directory
	 * NOTE: Since we still need to determine the log.dir, we may not log anything here!
	 */
	@NonNull
	private static Path findProjectDir() throws IOException {
		Path runFromDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		return validateIfEclipseOrIntelliJ(runFromDir);
	}

	/**
	 * Eclipse runs from the module (relative) directory.
	 * IntelliJ runs from the project root directory.
	 * NOTE: Since we still need to determine the log.dir, we may not log anything here!
	 */
	private static @NonNull Path validateIfEclipseOrIntelliJ(Path runFromDir) throws IOException {
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

	private static class ApplicationInitializerWrapper implements ServletContextInitializer {
		@Override
		public void onStartup(@NonNull ServletContext servletContext) throws ServletException {
			FrankApplicationInitializer init = new FrankApplicationInitializer();
			init.onStartup(servletContext);
			LogManager.getLogger("APPLICATION").info("Started Frank!Application");
		}
	}

	private static class LadybugInitializerWrapper implements ServletContextInitializer {
		@Override
		public void onStartup(@NonNull ServletContext servletContext) throws ServletException {
			System.setProperty("ladybug.jdbc.datasource", "");
			LadybugWarInitializer init = new LadybugWarInitializer();
			init.onStartup(servletContext);
			LogManager.getLogger("APPLICATION").info("Started Ladybug");
		}
	}

	private static class ConsoleInitializerWrapper implements ServletContextInitializer {
		@Override
		public void onStartup(@NonNull ServletContext servletContext) throws ServletException {
			ConsoleWarInitializer init = new ConsoleWarInitializer();
			init.onStartup(servletContext);
			LogManager.getLogger("APPLICATION").info("Started Console");
		}
	}

	/** Required to enable the use of WebSockets when starting as (Spring)Boot-able application. */
	private static class WsSciWrapper implements ServletContextInitializer {
		@Override
		public void onStartup(@NonNull ServletContext servletContext) {
			WsContextListener sc = new WsContextListener();
			sc.contextInitialized(new ServletContextEvent(servletContext));
		}
	}

	private static class ConfigureAppConstants implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource(SpringContextScope.ENVIRONMENT.getFriendlyName(), AppConstants.getInstance()));
		}
	}

	/**
	 * Disable the Spring-Boot LoggingSystem.
	 * Spring programmatically adds Console appenders and configures it's format regardless of what we configure.
	 *
	 * See {@link Log4J2LoggingSystem#initialize(org.springframework.boot.logging.LoggingInitializationContext, String, org.springframework.boot.logging.LogFile)}.
	 */
	private static void disableSpringBootLogging() {
		LoggerContext logContext = LoggerContext.getContext(false);
		logContext.setExternalContext(LoggingSystem.class.getName());
	}

	private static void disableAuthentication() {
		System.setProperty("application.security.http.authentication", "false");
		System.setProperty("application.security.http.transportGuarantee", "none");
		System.setProperty("dtap.stage", "LOC");
	}

	/**
	 * When the application fails to start up, trigger a shutdown and log the exception.
	 */
	@Log4j2
	private static class FailedInitializationMonitor implements ApplicationListener<ApplicationFailedEvent> {

		@Override
		public void onApplicationEvent(ApplicationFailedEvent event) {
			log.fatal("unable to start application", event.getException());
			LogUtil.getLogger("APPLICATION").fatal("unable to start application: {}", () -> getRootCause(event.getException()).getMessage());

			System.exit(1); // Terminate the JVM
		}

		private Throwable getRootCause(Throwable t) {
			if (t instanceof BeansException) {
				return (t.getCause() != null) ? getRootCause(t.getCause()) : t;
			}
			return t;
		}
	}
}
