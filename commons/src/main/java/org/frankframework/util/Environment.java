/*
   Copyright 2023-2025 WeAreFrank!

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
package org.frankframework.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.annotation.Nonnull;

public class Environment {
	private static final Logger log = LogManager.getLogger(Environment.class);

	public static Properties getEnvironmentVariables() throws IOException {
		Properties props = new Properties();

		try {
			System.getenv().forEach(props::setProperty);
		} catch (Exception e) {
			getLogger().debug("Exception getting environment variables", e);
		}

		if (!props.isEmpty()) {
			return props;
		}
		return readEnvironmentFromOsCommand();

	}

	private static Properties readEnvironmentFromOsCommand() throws IOException {
		Properties props = new Properties();
		String command = determineOsSpecificEnvCommand();
		getLogger().debug("Reading environment variables from OS using command [{}]", command);
		Runtime r = Runtime.getRuntime();
		Process p = r.exec(command);

		BufferedReader br = new BufferedReader(StreamUtil.getCharsetDetectingInputStreamReader(p.getInputStream()));
		String line;
		while ((line = br.readLine()) != null) {
			int idx = line.indexOf('=');
			if (idx >= 0) {
				String key = line.substring(0, idx);
				String value = line.substring(idx + 1);
				props.setProperty(key, value);
			}
		}
		return props;
	}

	private static Logger getLogger() {
		// Delay getting the Log manager configuration may depend on configuration code calling this class.
		return LogManager.getLogger(Environment.class);
	}

	private static String determineOsSpecificEnvCommand() {
		String OS = System.getProperty("os.name").toLowerCase();
		String envCommand;
		if (OS.contains("windows 9")) {
			envCommand = "command.com /c set";
		} else if (
				(OS.contains("nt"))
						|| (OS.contains("windows 20"))
						|| (OS.contains("windows xp"))) {
			envCommand = "cmd.exe /c set";
		} else {
			// assume Unix
			envCommand = "env";
		}
		return envCommand;
	}

	/**
	 * Look up the property in the environment with {@link System#getenv(String)} and next in the System properties with
	 * {@link System#getProperty(String, String)}. The {@link SecurityException} if thrown, is hidden.
	 *
	 * @param property The property to search for.
	 * @param def      The default value to return, may be {@code null}.
	 * @return the string value of the system property, or the default value if
	 * 		there is no property with that property.
	 * 		May return {@code null} if the default value was {@code null}.
	 * @since 1.1
	 */
	public static Optional<String> getSystemProperty(String property, String def) {
		try {
			String result = System.getenv().get(property);
			if (result != null) {
				return Optional.of(result);
			}
		} catch (Throwable e) {
			getLogger().warn("Was not allowed to read environment variable [{}]: {}", property, e.getMessage());
		}
		try {
			return Optional.ofNullable(System.getProperty(property, def));
		} catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			getLogger().warn("Was not allowed to read system property [{}]: {}", property, e.getMessage());
			return Optional.ofNullable(def);
		}
	}

	@Nonnull
	public static Manifest getManifest(@Nonnull URL jarFileLocation) throws IOException {
		try (JarFile file = new JarFile(jarFileLocation.getFile())) {
			Manifest manifest = file.getManifest();
			if (manifest == null) {
				throw new IOException("unable to find manifest file");
			}
			log.info("found {} in {}", JarFile.MANIFEST_NAME, jarFileLocation);
			return manifest;
		} catch (IOException e) {
			log.warn("unable to read " + JarFile.MANIFEST_NAME, e);
			throw e;
		}
	}
}
