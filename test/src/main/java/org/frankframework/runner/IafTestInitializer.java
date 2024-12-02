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
package org.frankframework.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import org.apache.logging.log4j.LogManager;
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

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
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

	public static class ConfigureAppConstants implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource(SpringContextScope.ENVIRONMENT.getFriendlyName(), AppConstants.getInstance()));
		}
	}

	public static void main(String[] args) throws IOException {
		Path runFromDir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
		Path projectDir = validateIfEclipseOrIntelliJ(runFromDir);

		getConfigurationsDirectory(projectDir);

		System.setProperty("application.security.http.authentication", "false");
		System.setProperty("application.security.http.transportGuarantee", "none");
		System.setProperty("dtap.stage", "LOC");
		System.setProperty("active.jms", "false");
		System.setProperty("log.dir", getLogDir(projectDir));
		System.setProperty(ApplicationServerConfigurer.APPLICATION_SERVER_TYPE_PROPERTY, "IBISTEST");

		SpringApplication app = new SpringApplication();
		app.addInitializers(new ConfigureAppConstants());
		app.setWebApplicationType(WebApplicationType.SERVLET);
		Set<String> set = new HashSet<>();
		app.addPrimarySources(List.of(LadybugInitializerWrapper.class, ApplicationInitializerWrapper.class, ConsoleInitializerWrapper.class));
		set.add(SpringContextScope.ENVIRONMENT.getContextFile());
		set.add("TestFrankContext.xml");
		app.setSources(set);
		app.run(args);
	}

	private static void getConfigurationsDirectory(Path projectDir) throws IOException {
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

	// Eclipse runs from the module (relative) directory.
	// IntelliJ runs from the project root directory.
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
	private static String getLogDir(Path projectPath) throws IOException {
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
