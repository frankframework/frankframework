/*
   Copyright 2026 WeAreFrank!

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
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.frankframework.lifecycle.servlets.ApplicationServerConfigurer;

/**
 * Spring Boot entrypoint or main class defined in the pom.xml when packaging using the 'spring-boot:repackage' goal.
 *
 * @author Niels Meijer
 */
// Careful.. don't log here!!
public class IafTestInitializer {

	public static void main(String[] args) throws IOException {
		FrankApplication app = configureApplication();
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
	static FrankApplication configureApplication(@Nullable String appServerCustom, @Nullable String dbms, @Nullable String jmsProvider) throws IOException {
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
	static FrankApplication configureApplication() throws IOException {
		FrankApplication frankApp = new FrankApplication();
		// Find and configure the configurations
		setConfigurationsDirectory(frankApp.getProjectDir());

		// Configure JMS
		String jmsProvider = System.getProperty("jms.provider.default");
		// ServerType "IBISTEST" disables JMS by default, so we need to override it depending on if this property has been set.
		System.setProperty("active.jms", jmsProvider != null ? "true" : "false");
		if ("inmem".equals(jmsProvider)) {
			System.setProperty("active.amqp", "false");
		} else {
			System.setProperty("active.amqp", jmsProvider != null ? "true" : "false");
		}
		if (jmsProvider != null) {
			// Setting these properties manually is required with application-server type "IBISTEST"
			System.setProperty("jms.connectionfactory.qcf." + jmsProvider, "jms/qcf-" + jmsProvider);
			System.setProperty("jms.destination.suffix", "-" + jmsProvider);
			System.setProperty("amqp.connection", "amqp/" + jmsProvider);
		}

		String dbms = System.getProperty("jdbc.dbms.default");
		if (dbms != null && !"h2".equals(dbms)) {
			System.setProperty("active.storedProcedureTests", "true");
		}

		// Configure a CredentialProvider
		frankApp.configureCredentialProvider("src/main/secrets/credentials.properties");
		System.setProperty("authAliases.expansion.allowed", "testalias");

		return frankApp;
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
}
