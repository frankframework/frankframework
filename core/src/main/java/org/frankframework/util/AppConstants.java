/*
   Copyright 2013, 2016 - 2019 Nationale-Nederlanden, 2020 - 2023 WeAreFrank!

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

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.frankframework.lifecycle.servlets.ApplicationServerConfigurer;

/**
 * Configuration Constants for this application.
 *
 * <p>When an instance is created, it tries to load the properties file specified
 * by the <code>propertiesFileName</code> field</p>.
 * <p>If a property exits with the name <code>ADDITIONAL.PROPERTIES.FILE</code>
 * that file is loaded also</p>
 *
 * <p>
 *     There is a global instance that can be retrieved with {@link AppConstants#getInstance()}, and an
 *     instance per configuration that can be retrieved with {@link AppConstants#getInstance(ClassLoader)}
 *     using the configuration classloader. Each method will the instance if it does not yet exist.
 * </p>
 * <p>
 *     To change a value across all instances, use {@link AppConstants#setGlobalProperty(String, String)}. Changing
 *     a value for only a single instance with {@link AppConstants#setProperty(String, String)} is in general recommended
 *     only for tests.
 * </p>
 * @author Niels Meijer
 * @version 2.1
 *
 */
public final class AppConstants extends PropertyLoader {
	private static final Logger LOG = LogManager.getLogger(AppConstants.class);

	private static final String APP_CONSTANTS_PROPERTIES_FILE = "AppConstants.properties";
	private static final String ADDITIONAL_PROPERTIES_FILE_KEY = "ADDITIONAL.PROPERTIES.FILE";
	public static final String APPLICATION_SERVER_TYPE_PROPERTY = ApplicationServerConfigurer.APPLICATION_SERVER_TYPE_PROPERTY;
	public static final String APPLICATION_SERVER_CUSTOMIZATION_PROPERTY = ApplicationServerConfigurer.APPLICATION_SERVER_CUSTOMIZATION_PROPERTY;
	public static final String ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY = ADDITIONAL_PROPERTIES_FILE_KEY+".SUFFIX";

	private static final Properties globalAppConstants = new Properties();

	private static final ConcurrentHashMap<Integer, AppConstants> appConstantsMap = new ConcurrentHashMap<>();

	private AppConstants(ClassLoader classLoader) {
		super(classLoader, APP_CONSTANTS_PROPERTIES_FILE);

		// Calculate this outside the cleaner-function to make sure we do not accidentally capture a reference
		// to it for forever
		int key = System.identityHashCode(classLoader);
		// Register a cleaning-function so that when the classloader goes out of scope, the AppConstants instance
		// will be automatically removed.
		// This prevents leaks of ClassLoader and AppConstants instances.
		CleanerProvider.register(classLoader, () -> appConstantsMap.remove(key));

		// Add all global properties
		// This is a bit of an ugly hack, but otherwise these properties cannot be retrieved via get/getProperty methods
		putAll(globalAppConstants);
	}

	/**
	 * Return the AppConstants root instance
	 * @return AppConstants instance
	 */
	public static AppConstants getInstance() {
		return getInstance(AppConstants.class.getClassLoader());
	}

	/**
	 * Retrieve an instance based on a ClassLoader. This should be used by
	 * classes which are part of the Ibis configuration (like pipes and senders)
	 * because the configuration might be loaded from outside the webapp
	 * classpath. Hence, the Thread.currentThread().getContextClassLoader() at
	 * the time the class was instantiated should be used.
	 *
	 * @param classLoader ClassLoader to retrieve AppConstants from
	 * @return AppConstants instance
	 */
	public static synchronized AppConstants getInstance(final ClassLoader classLoader) {
		if(classLoader == null) {
			throw new IllegalStateException("calling AppConstants.getInstance without ClassLoader");
		}

		return appConstantsMap.computeIfAbsent(System.identityHashCode(classLoader), k-> new AppConstants(classLoader));
	}

	public static void removeInstance() {
		removeInstance(AppConstants.class.getClassLoader());
	}

	public static synchronized void removeInstance(final ClassLoader cl) {
		if(cl == null) {
			throw new IllegalStateException("calling AppConstants.removeInstance without ClassLoader");
		}
		int key = System.identityHashCode(cl);
		AppConstants instance = appConstantsMap.get(key);
		if (instance != null) {
			appConstantsMap.remove(key);
		}
	}

	/**
	 * Returns a list of {@link AppConstants#getInstance() AppConstants} which names begin with the keyBase
	 */
	public Properties getAppConstants(String keyBase) {
		return getAppConstants(keyBase, true, true);
	}

	/**
	 * Returns a list of {@link AppConstants#getInstance() AppConstants} which names begin with the keyBase
	 */
	public Properties getAppConstants(String keyBase, boolean useSystemProperties, boolean useEnvironmentVariables) {
		final String propertyPrefix = keyBase + (!keyBase.endsWith(".") ? "." : "");

		AppConstants constants = getInstance();
		if(useSystemProperties)
			constants.putAll(System.getProperties());
		if(useEnvironmentVariables) {
			try {
				constants.putAll(Environment.getEnvironmentVariables());
			} catch (IOException e) {
				LOG.warn("unable to retrieve environment variables", e);
			}
		}

		Properties filteredProperties = new Properties();
		for(Object objKey: constants.keySet()) {
			String key = (String) objKey;
			if(key.startsWith(propertyPrefix)) {
				filteredProperties.put(key, constants.getResolvedProperty(key));
			}
		}

		return filteredProperties;
	}

	/**
	 * Use this method for testing only, when only local properties need to be set that do not
	 * affect other tests.
	 *
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to {@code key}.
	 * @return Previous value, or null
	 */
	@Override
	public synchronized Object setProperty(String key, String value) {
		return super.setProperty(key, value);
	}

	/**
	 * Set key as boolean. Use this method for testing only, when local properties need to be set
	 * that do not affect other tests.
	 *
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to {@code key}.
	 * @return Previous value, or null
	 */
	public Object setProperty(String key, boolean value) {
		return setProperty(key, ""+value);
	}

	/**
	 * Set boolean value in all instances of AppConstants, current and future.
	 * Use this in production-code to make sure a settings-change is propagated to all configurations.
	 *
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to {@code key}.
	 * @return Previous value, or null
	 */
	public static @Nullable Boolean setGlobalProperty(@Nonnull String key, boolean value) {
		String retval = setGlobalProperty(key, "" + value);
		if (retval == null) {
			return null;
		}
		return Boolean.parseBoolean(retval);
	}

	/**
	 * Set value in all instances of AppConstants, current and future.
	 * Use this in production-code to make sure a settings-change is propagated to all configurations.
	 *
	 * @param key the key to be placed into this property list.
	 * @param value the value corresponding to {@code key}.
	 * @return Previous value, or null
	 */
	public static synchronized @Nullable String setGlobalProperty(@Nonnull String key, @Nonnull String value) {
		// Copying global app-constant values to all instances is a bit of a hack but there's not much else we can do
		for (AppConstants localAppConstants : appConstantsMap.values()) {
			localAppConstants.setProperty(key, value);
		}
		//Store in a map in case a new AppConstants instance is created after the property has already been set
		return (String)globalAppConstants.put(key, value);
	}

	/**
	 * Clear a property in all instances of AppConstants.
	 *
	 * @param key the key to be removed
	 * @return The value associated with the key, or null
	 */
	public static synchronized @Nullable String clearGlobalProperty(@Nonnull String key) {
		for (AppConstants localAppConstants : appConstantsMap.values()) {
			localAppConstants.remove(key);
		}
		return (String)globalAppConstants.remove(key);
	}

	@Override
	protected synchronized void load(ClassLoader classLoader, String filename) {
		load(classLoader, filename, true);
	}

	/**
	 * Load the contents of a properties file.
	 * <p>Optionally, this may be a comma-separated list of files to load, e.g.
	 * <code>log4j2.properties,deploymentspecifics.properties</code>
	 * which will cause both files to be loaded in the listed order.
	 * </p>
	 */
	private synchronized void load(ClassLoader classLoader, String filename, boolean loadAdditionalPropertiesFiles) {
		load(classLoader, filename, null, loadAdditionalPropertiesFiles);
	}

	private synchronized void load(final ClassLoader classLoader, final String filename, final String suffix, final boolean loadAdditionalPropertiesFiles) {
		for (final String theFilename : StringUtil.split(filename)) {
			super.load(classLoader, theFilename);

			String loadFile = getProperty(ADDITIONAL_PROPERTIES_FILE_KEY); //Only load additional properties if it's defined...
			if (loadAdditionalPropertiesFiles && StringUtils.isNotEmpty(loadFile)) {
				// Add properties after load(is) to prevent load(is)
				// from overriding them
				String loadFileSuffix = getProperty(ADDITIONAL_PROPERTIES_FILE_SUFFIX_KEY);
				if (StringUtils.isNotEmpty(loadFileSuffix)){
					load(classLoader, loadFile, loadFileSuffix, false);
				} else {
					load(classLoader, loadFile, false);
				}
			}

			if (suffix != null) {
				String baseName = FilenameUtils.getBaseName(theFilename);
				String extension = FilenameUtils.getExtension(theFilename);
				String suffixedFilename = baseName
						+ "_"
						+ suffix
						+ (StringUtils.isEmpty(extension) ? "" : "."
								+ extension);
				load(classLoader, suffixedFilename, false);
			}
		}
	}
}
