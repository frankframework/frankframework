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
 * Singleton class that has the constant values for this application. <br/>
 * <p>When an instance is created, it tries to load the properties file specified
 * by the <code>propertiesFileName</code> field</p>
 * <p>If a property exits with the name <code>ADDITIONAL.PROPERTIES.FILE</code>
 * that file is loaded also</p>

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

	private static final ConcurrentHashMap<ClassLoader, AppConstants> appConstantsMap = new ConcurrentHashMap<>();

	private AppConstants(ClassLoader classLoader) {
		super(classLoader, APP_CONSTANTS_PROPERTIES_FILE);

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

		return appConstantsMap.computeIfAbsent(classLoader, AppConstants::new);
	}

	public static void removeInstance() {
		removeInstance(AppConstants.class.getClassLoader());
	}

	public static synchronized void removeInstance(final ClassLoader cl) {
		if(cl == null) {
			throw new IllegalStateException("calling AppConstants.removeInstance without ClassLoader");
		}
		AppConstants instance = appConstantsMap.get(cl);
		if (instance != null) {
			appConstantsMap.remove(cl);
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

	public Object setProperty(String key, boolean value) {
		return setProperty(key, ""+value);
	}

	public static @Nullable Boolean setGlobalProperty(@Nonnull String key, boolean value) {
		String retval = setGlobalProperty(key, "" + value);
		if (retval == null) {
			return null;
		}
		return Boolean.parseBoolean(retval);
	}

	public static synchronized @Nullable String setGlobalProperty(@Nonnull String key, @Nonnull String value) {
		// Copying global app-constant values to all instances is a bit of a hack but there's not much else we can do
		for (AppConstants localAppConstants : appConstantsMap.values()) {
			localAppConstants.setProperty(key, value);
		}
		//Store in a map in case a new AppConstants instance is created after the property has already been set
		return (String)globalAppConstants.put(key, value);
	}

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
