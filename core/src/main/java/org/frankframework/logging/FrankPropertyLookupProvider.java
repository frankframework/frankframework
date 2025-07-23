/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.logging;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Properties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.ByteOrderMark;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;
import org.apache.logging.log4j.status.StatusLogger;

import org.frankframework.util.StringResolver;

/**
 * Add the Frank property resolver to the log configuration context. Properties starting with {@code ff:} will be substituted via this LookupProvider.
 * 
 * NOTE:
 * The use of Lombok is not allowed, this may break annotation processing! This is required now that package scanning is no longer allowed.
 * Should not depend on any (util) classes that use a logger!
 */
@Plugin(name = "ff", category = StrLookup.CATEGORY)
public class FrankPropertyLookupProvider extends AbstractLookup {
	private static final Logger LOGGER = StatusLogger.getLogger();
	private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");
	private static final String LOG4J_PROPS_FILE = "log4j4ibis.properties";
	private static final String DS_PROPERTIES_FILE = "DeploymentSpecifics.properties";

	private final Properties properties;

	public FrankPropertyLookupProvider() throws IOException {
		properties = getProperties();
	}

	@Override
	public String lookup(LogEvent ignored, String key) { // Always ignore the event
		String value = properties.getProperty(key);

		// We have to return a 'lookup' value, else it will not be resolved and the XML will break.
		if(StringUtils.isEmpty(value)) {
			return "";
		}

		if(StringResolver.needsResolution(value)) {
			value = StringResolver.substVars(value, properties);
		}
		return value;
	}

	@Nonnull
	protected Properties getProperties() throws IOException {
		Properties log4jProperties = getParseProperties(LOG4J_PROPS_FILE);
		if(log4jProperties == null) {
			log4jProperties = new Properties();

			LOGGER.warn(LOOKUP, "did not find " + LOG4J_PROPS_FILE + ", leaving it up to log4j's default initialization procedure");
		}

		Properties dsProperties = getParseProperties(DS_PROPERTIES_FILE);
		if (dsProperties != null) {
			log4jProperties.putAll(dsProperties);
		}

		log4jProperties.putAll(System.getProperties()); // Set these after reading DeploymentSpecifics as we want to override the properties
		log4jProperties.putAll(System.getenv()); // let environment properties override system properties and appConstants
		setInstanceNameLc(log4jProperties); // Set instance.name.lc for log file names
		setLevel(dsProperties); // Set the log.level if it does not exist yet

		return log4jProperties;
	}

	private @Nullable Properties getParseProperties(String filename) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		Enumeration<URL> urls = cl.getResources(filename);
		URL url = null;
		while (urls.hasMoreElements()) {
			url = urls.nextElement();
		}

		if(url != null) {
			Properties properties = new Properties();
			try(InputStream is = url.openStream(); Reader reader = getCharsetDetectingInputStreamReader(is)) {
				properties.load(reader);
			}
			return properties;
		}
		return null;
	}

	/* May be duplicate code, but the LogFactory may not depend on any class that uses a logger. */
	private static Reader getCharsetDetectingInputStreamReader(InputStream inputStream) throws IOException {
		BOMInputStream bOMInputStream = BOMInputStream.builder()
				.setInputStream(inputStream)
				.setByteOrderMarks(ByteOrderMark.UTF_8, ByteOrderMark.UTF_16LE, ByteOrderMark.UTF_16BE)
				.get();

		ByteOrderMark bom = bOMInputStream.getBOM();
		String charsetName = bom == null ? StandardCharsets.UTF_8.displayName() : bom.getCharsetName();

		return new InputStreamReader(new BufferedInputStream(bOMInputStream), charsetName);
	}

	/**
	 * Adds an additional property with a lower-case version of the instance.name.
	 */
	private static void setInstanceNameLc(Properties log4jProperties) {
		String instanceName = log4jProperties.getProperty("instance.name");
		String instanceNameLowerCase = (instanceName != null) ? instanceName.toLowerCase() : "ibis";

		log4jProperties.setProperty("instance.name.lc", instanceNameLowerCase);
	}

	/**
	 * Checks if the {@code log.level} is set in the system properties.
	 * If not set, sets it based on {@code dtap.stage}: When system property {@code dtap.stage}
	 * is {@code ACC} or {@code PRD} then the log level is set to {@code WARN}, otherwise to {@code INFO}.
	 */
	private static void setLevel(Properties properties) {
		if (properties.getProperty("log.level") == null) {
			// In the log4j4ibis.xml the rootlogger contains the loglevel: ${log.level}
			// You can set this property in the log4j4ibis.properties, or as system property.
			// To make sure the IBIS can start up if no log.level property has been found, it has to be explicitly set
			String stage = properties.getProperty("dtap.stage");
			String logLevel = "INFO";
			if("ACC".equalsIgnoreCase(stage) || "PRD".equalsIgnoreCase(stage)) {
				logLevel = "WARN";
			}
			properties.setProperty("log.level", logLevel);
			System.setProperty("log.level", logLevel);
		}
	}
}
