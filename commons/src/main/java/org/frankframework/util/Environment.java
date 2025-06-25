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
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.util.Optional;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipInputStream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Environment {
	private static final Logger log = LogManager.getLogger(Environment.class);
	private static final String FRANKFRAMEWORK_NAMESPACE = "META-INF/maven/org.frankframework/";

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

	/**
	 * Get FF module version based on the pom.properties file.
	 *
	 * @param module name of the module to fetch the version
	 * @return module version or null if not found
	 */
	public static @Nullable String getModuleVersion(@Nonnull String module) {
		ClassLoader classLoader = Environment.class.getClassLoader();
		URL pomProperties = classLoader.getResource(FRANKFRAMEWORK_NAMESPACE + module + "/pom.properties");

		if (pomProperties == null) {
			// unable to find module, assume it's not on the classpath
			return null;
		}
		try (InputStream is = pomProperties.openStream()) {
			Properties props = new Properties();
			props.load(is);
			return (String) props.get("version");
		} catch (IOException e) {
			log.warn("unable to read pom.properties file for module [{}]", module, e);

			return "unknown";
		}
	}

	@Nonnull
	public static Manifest getManifest(@Nonnull URL jarFileLocation) throws IOException {
		try (JarInputStream jarInputStream = new JarInputStream(jarFileLocation.openStream())) {
			Manifest manifest = jarInputStream.getManifest();
			if (manifest == null) {
				manifest = getManifestFromFile(jarFileLocation);
			}
			log.debug("found {} in {}", JarFile.MANIFEST_NAME, jarFileLocation);
			return manifest;
		} catch (IOException e) {
			log.info("unable to read " + JarFile.MANIFEST_NAME, e);
			throw e;
		}
	}

	protected static Manifest getManifestFromRelativeLocation(URL pomProperties) throws IOException {
		String fullUrl = pomProperties.toExternalForm();
		int metaInfFolder = fullUrl.indexOf("/META-INF/");
		if (metaInfFolder > 0) { // We found the meta-inf folder, it could be a shared classpath. Attempt to locate the MANIFEST file.
			String manifestFile = fullUrl.substring(fullUrl.indexOf(pomProperties.getProtocol()), metaInfFolder +1) + JarFile.MANIFEST_NAME;
			URL manifestURL = new URL(pomProperties, manifestFile);
			log.debug("attempting to read manifest at location [{}]", manifestURL);
			try (InputStream is = manifestURL.openStream()) { // Throws a FileNotFoundException
				return new Manifest(is);
			}
		}
		throw new FileNotFoundException("no manifest file found relative to path ["+pomProperties+"]");
	}

	/**
	 * Fallback to read the Jar using a File handle.
	 * When the first entry is not the Manifest file, the {@link ZipInputStream} will return null when calling `getManifest`.
	 * Ideally the Manifest file is always generated via Maven (or any other build tool) but it could be manipulated in the CI.
	 * This fallback mechanism works (obviously) only when it's a local file, which it should be regardless.
	 */
	@Nonnull
	private static Manifest getManifestFromFile(@Nonnull URL jarFileLocation) throws IOException {
		String cleanPath = extractPath(jarFileLocation);
		try (JarFile jarFile = new JarFile(cleanPath)) {
			Manifest manifest = jarFile.getManifest();
			if (manifest == null) {
				throw new NoSuchFileException("unable to find manifest file");
			}
			return manifest;
		}
	}

	public static String extractPath(final URL url) throws IOException {
		String urlPath = url.getPath(); // same as getFile but without the Query portion

		// I would be surprised if URL.getPath() ever starts with "jar:" but no harm in checking
		if (urlPath.startsWith("jar:")) {
			urlPath = urlPath.substring(4);
		}
		// For jar: URLs, the path part starts with "file:"
		if (urlPath.startsWith("file:")) {
			urlPath = urlPath.substring(5);
		}

		if ("vfs".equals(url.getProtocol())) {
			return urlPath;
		}

		try {
			final String cleanPath = new URI(urlPath).getPath();
			if (new File(cleanPath).exists()) {
				// if URL-encoded file exists, don't decode it
				return cleanPath;
			}
			return URLDecoder.decode(urlPath, StandardCharsets.UTF_8.name());
		} catch (URISyntaxException e) {
			throw new IOException("unable to read path from URL ["+url+"]", e);
		}
	}
}
