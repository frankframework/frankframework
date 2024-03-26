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

import org.frankframework.lifecycle.ApplicationServerConfigurer;
import org.frankframework.lifecycle.FrankApplicationInitializer;
import org.frankframework.lifecycle.SpringContextScope;
import org.frankframework.util.AppConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
public class IafTestInitializer {

	public static class ApplicationInitializer extends FrankApplicationInitializer implements ServletContextInitializer {}

	public static class ConfigureAppConstants implements ApplicationContextInitializer<ConfigurableApplicationContext> {

		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {
			applicationContext.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource(SpringContextScope.ENVIRONMENT.getFriendlyName(), AppConstants.getInstance()));
		}
	}

	// Should start a XmlServletWebServerApplicationContext.
	public static void main(String[] args) throws IOException {
		String logDir = getLogDir();

		System.setProperty("application.security.http.authentication", "false");
		System.setProperty("application.security.http.transportGuarantee", "none");
		System.setProperty("dtap.stage", "LOC");
		System.setProperty("log.dir", logDir);
		System.setProperty(ApplicationServerConfigurer.APPLICATION_SERVER_TYPE_PROPERTY, "TOMCAT");

		SpringApplication app = new SpringApplication();
		app.addInitializers(new ConfigureAppConstants());
		app.setWebApplicationType(WebApplicationType.SERVLET);
		Set<String> set = new HashSet<>();
		app.addPrimarySources(List.of(ApplicationInitializer.class));
		set.add(SpringContextScope.ENVIRONMENT.getContextFile());
		set.add("TestFrankContext.xml");
		app.setSources(set);
		app.run(args);
	}

	private static String getLogDir() throws IOException {
		String currentDir = System.getProperty("user.dir");
		Path absPath = Path.of(currentDir).toAbsolutePath();
		Path targetPath = absPath.resolve("target");
		if(Files.exists(targetPath) && Files.isDirectory(targetPath)) {
			Path logDir = targetPath.resolve("logs");
			if(!Files.exists(logDir)) {
				Files.createDirectory(logDir);
			}
			return logDir.toAbsolutePath().toString();
		}
		throw new IOException("unable to determine log directory");
	}

}
