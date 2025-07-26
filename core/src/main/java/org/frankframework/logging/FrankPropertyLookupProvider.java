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
import java.lang.ref.SoftReference;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
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
	private static final String LOG_LEVEL_KEY = "log.level";

	private static SoftReference<Properties> propertiesRef = null;

	public FrankPropertyLookupProvider() throws IOException {
		getProperties(); // Load once, throw potential errors if any...
	}

	/**
	 * Cache the properties once, let the GC clean this up whenever possible, we can just load the properties again if needed.
	 * 
	 * Each Log4j2 configuration loads all plugins, which would otherwise cause this plugin to load the properties over and over again.
	 * This allows them to be cached, if the configuration is reloaded at a later time, and the `reference` might be gone we can load
	 * the properties again.
	 * 
	 * @return a cached instance of the Frank!Framework properties
	 */
	private static synchronized Properties getProperties() {
		if (propertiesRef == null || propertiesRef.get() == null) {
			Properties properties;
			try {
				properties = computeProperties();
				LOGGER.info(LOOKUP, "FrankPropertyLookupProvider finished loading Frank!Framework properties");
			} catch (IOException e) {
				LOGGER.fatal(LOOKUP, "FrankPropertyLookupProvider unable to load Frank!Framework properties", e);
				properties = new Properties();
			}

			propertiesRef = new SoftReference<>(properties);
		}
		return propertiesRef.get();
	}

	@Override
	public String lookup(LogEvent ignored, String key) { // Always ignore the event
		Properties properties = getProperties();
		String value = properties.getProperty(key);

		// Default values are handled by Log4j2 and will only work when the lookup returns {@code null}.
		// If a default value does not exist Log4k2 will use the key as value, e.g. the key `index` will get value `ff:index`.
		if(StringUtils.isEmpty(value)) {
			if ("log.dir".equals(key)) {
				System.err.println("big oops");
			}
			return null;
		}

		if(StringResolver.needsResolution(value)) {
			value = StringResolver.substVars(value, properties);
		}

		LOGGER.debug(LOOKUP, "FrankPropertyLookupProvider found key ["+key+"] and resolved it to ["+value+"]");
		return value;
	}

	@Nonnull
	protected static Properties computeProperties() throws IOException {
		Properties log4jProperties = getParseProperties(LOG4J_PROPS_FILE);
		if(log4jProperties == null) {
			log4jProperties = new Properties();

			LOGGER.warn(LOOKUP, "FrankPropertyLookupProvider did not find " + LOG4J_PROPS_FILE + ", leaving it up to log4j's default initialization procedure");
		}

		Properties dsProperties = getParseProperties(DS_PROPERTIES_FILE);
		if (dsProperties != null) {
			log4jProperties.putAll(dsProperties);
		}

		log4jProperties.putAll(System.getProperties()); // Set these after reading DeploymentSpecifics as we want to override the properties
		log4jProperties.putAll(System.getenv()); // let environment properties override system properties and appConstants
		setInstanceNameLc(log4jProperties); // Set instance.name.lc for log file names
		setLevel(log4jProperties); // Set the log.level if it does not exist yet

		return log4jProperties;
	}

	/**
	 * Sort entries `EXTERNAL CLASSPATH` > `WEB-INF/CLASSES` > `JAR FILES`.
	 * Other files should keep their order.
	 */
	protected static class UrlLocationComparator implements java.util.Comparator<URL> {
		@Override
		public int compare(URL o1, URL o2) {
			int o1i = 0;
			int o2i = 0;

			if (o1.toExternalForm().contains("/opt/frank/resources/")) o1i += -2;
			if (o2.toExternalForm().contains("/opt/frank/resources/")) o2i += -2;

			if (o1.toExternalForm().contains("/WEB-INF/classes/")) o1i += -1;
			if (o2.toExternalForm().contains("/WEB-INF/classes/")) o2i += -1;

			return o1i - o2i;
		}
	}

	/**
	 * Scan the classpath and find the correct resource to use.
	 * See the UrlLocationComparator for the order.
	 */
	private static URL findResource(String filename) throws IOException {
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		List<URL> urls = Collections.list(cl.getResources(filename));
		urls.sort(new UrlLocationComparator());
		return urls.get(0);
	}

	private static @Nullable Properties getParseProperties(String filename) throws IOException {
		URL url = findResource(filename);

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
		if (properties.getProperty(LOG_LEVEL_KEY) == null) {
			// In the log4j4ibis.xml the rootlogger contains the loglevel: ${log.level}
			// You can set this property in the log4j4ibis.properties, or as system property.
			// To make sure the IBIS can start up if no log.level property has been found, it has to be explicitly set
			String stage = properties.getProperty("dtap.stage");
			String logLevel = "INFO";
			if("ACC".equalsIgnoreCase(stage) || "PRD".equalsIgnoreCase(stage)) {
				logLevel = "WARN";
			}
			properties.setProperty(LOG_LEVEL_KEY, logLevel);
			System.setProperty(LOG_LEVEL_KEY, logLevel);
		}
	}
}
