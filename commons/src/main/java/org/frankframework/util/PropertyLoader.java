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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serial;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Properties;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PropertyLoader extends Properties {

	@Serial
	private static final long serialVersionUID = 1L;
	private final String rootPropertyFile;

	public PropertyLoader(File propertiesFile, Properties defaults) throws IOException {
		super(defaults);
		rootPropertyFile = propertiesFile.getAbsolutePath();
		loadResource(propertiesFile.toURI().toURL());
//		loadFile(propertiesFile);
	}

	public PropertyLoader(String propertiesFile) {
		this(PropertyLoader.class.getClassLoader(), propertiesFile);
	}

	public PropertyLoader(ClassLoader classLoader, String propertiesFile) {
		super();
		rootPropertyFile = propertiesFile;

		load(classLoader, propertiesFile);

		// Make sure to not call ClassUtils when using the root instance, as it has a static field referencing to AppConstants
		if (classLoader != null) {
			log.info("created new PropertyLoader for classloader [{}]", () -> ClassUtils.classNameOf(classLoader));
		} else {
			log.info("created new PropertyLoader for root classloader");
		}
	}

	/**
	 * Very similar to <code>System.getProperty</code> except
	 * that the {@link SecurityException} is hidden.
	 *
	 * @param key The key to search for.
	 * @return the string value of the system property, or NULL if there is no property with that key.
	 */
	private String getSystemProperty(String key) {
		try {
			String result = System.getenv().get(key);
			if (result != null) {
				log.trace("Get key [{}] from System Environment, value: [{}]", key, result);
				return result;
			}
		} catch (Throwable e) {
			log.warn("unable to read environment variable [{}]: {}", () -> key, e::getMessage);
		}
		try {
			String result = System.getProperty(key);
			if (result != null) {
				log.trace("Get key [{}] from System Properties, value: [{}]", key, result);
			}
			return result;
		} catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			log.warn("unable to read system property [{}]: {}", () -> key, e::getMessage);
			return null;
		}
	}

	@Override
	public synchronized String get(Object key) {
		return getResolvedProperty((String) key);
	}

	@Override
	public String getProperty(String key) {
		return getResolvedProperty(key);
	}

	public String getUnresolvedProperty(String key) {
		return super.getProperty(key);
	}

	@Override
	public final boolean containsKey(Object objKey) {
		if (!(objKey instanceof String key)) {
			return false;
		}

		String value = getSystemProperty(key); // First try system properties and environment variables.
		return value != null || super.containsKey(key); // Property could be unresolved but that's ok, it will exist and that's what we verify.
	}

	/**
	 * the method is like the <code>Properties.getProperty</code>, but provides functionality to resolve <code>${variable}</code>
	 * Syntaxes. It uses the property values and system values to resolve the variables, and does so recursively.
	 *
	 * @see StringResolver
	 */
	protected final String getResolvedProperty(String key) {
		String value = getSystemProperty(key); // first try system properties
		if (value == null) {
			value = super.getProperty(key); // then try AppConstants and DeploymentSpecifics
		}
		if (value != null) {
			try {
				if (value.contains(StringResolver.DELIM_START + key + StringResolver.DELIM_STOP)) {
					log.warn("cyclic property definition key [{}] value [{}]", key, value);
					return value;
				}
				String result = StringResolver.substVars(value, this);
				if (!value.equals(result)) {
					log.trace("substituted key [{}] with value from [{}] to [{}]", key, value, result);
				}

				log.trace("getResolvedProperty: key [{}] resolved to value [{}]", key, value);
				return result;
			} catch (IllegalArgumentException e) {
				log.error("bad option value [{}] for key [{}]", value, key, e);
				return value;
			}
		} else {
			log.trace("getResolvedProperty: key [{}] was not found", key);
			return null;
		}
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getOrDefault(@Nonnull String key, @Nonnull T dfault) {
		String value = getProperty(key);
		if (value == null) {
			return dfault;
		}
		return (T) EnumUtils.parse(dfault.getClass(), value);
	}

	/**
	 * Retrieves a list property value associated with the specified key. The method first resolves the property value using
	 * the {@link #getResolvedProperty(String)} method. If the resolved property value is null, an empty list is returned.
	 *
	 * @param key the key of the property value to retrieve
	 * @return a list of string values associated with the specified key, or an empty list if the resolved property is null
	 */
	@Nonnull
	public List<String> getListProperty(@Nonnull String key) {
		return getListProperty(key, null);
	}

	/**
	 * Retrieves a list property value associated with the specified key. The method first resolves the property value using
	 * the {@link #getResolvedProperty(String)} method. If the resolved property value is null, it returns the list of string
	 * values provided as "defaults".
	 *
	 * @param key      the key of the property value to retrieve
	 * @param defaults the default list of string values to return if the resolved property is null
	 * @return a list of string values associated with the specified key, or the default list if the resolved property is null.
	 * 		If the defaults is also null, then returns an empty list.
	 */
	@Nonnull
	public List<String> getListProperty(@Nonnull String key, @Nullable String defaults) {
		String list = getResolvedProperty(key);
		if (list != null) {
			return StringUtil.split(list);
		}
		return StringUtil.split(defaults);
	}

	/**
	 * Add property only in the local PropertyLoader!
	 * Do not use this method and use {@link #setProperty(String, String)} if you want to set the property globally!
	 * <p>
	 * This method is used by {@link Properties#load(InputStream)} to add all properties found (in a file/stream)
	 * to the underlying {@link Hashtable}.
	 *
	 * @deprecated Use {@link #setProperty(String, String)} instead!
	 */
	@Deprecated
	@Override
	public synchronized Object put(Object key, Object value) {
		return super.put(key, value);
	}

	// Load methods

	/**
	 * Load the contents of a properties file.
	 * <p>Optionally, this may be a comma-separated list of files to load, e.g.
	 * <code>log4j2.properties,DeploymentSpecifics.properties</code>
	 * which will cause both files to be loaded in the listed order.
	 * </p>
	 */
	protected synchronized void load(final ClassLoader classLoader, final String filename) {
		if (StringUtils.isEmpty(filename)) {
			throw new IllegalStateException("file to load properties from cannot be null");
		}

		try {
			if (classLoader == null) {
				throw new IllegalStateException("no classloader found!");
			}
			List<URL> resources = Collections.list(classLoader.getResources(filename));
			if (resources.isEmpty()) {
				if (rootPropertyFile.equals(filename)) { // The file cannot be found, abort!
					String msg = rootPropertyFile + " file not found, unable to initialize PropertyLoader";
					log.error(msg);
					throw new MissingResourceException(msg, this.getClass().getSimpleName(), rootPropertyFile);
				}

				// An additional file to load properties from cannot be found
				log.debug("cannot find resource [{}] in classloader [{}] to load additional properties from, ignoring", filename, classLoader);
			}

			// We need to reverse the loading order to make sure the parent files are loaded first
			Collections.reverse(resources);

			for (URL url : resources) {
				loadResource(url);
				log.info("Properties loaded from url [{}]", url::toString);
			}
		} catch (IOException e) {
			log.error("error reading properties from [{}]", rootPropertyFile, e);
		}
	}

	// Special Getters

	/**
	 * Gets a <code>String</code> value
	 * Uses the {@link #getResolvedProperty(String)} method.
	 *
	 * @param key    the Key
	 * @param dfault the default value
	 * @return String
	 */
	public String getString(String key, String dfault) {
		String ob = this.getResolvedProperty(key);

		if (ob == null) return dfault;
		return ob;
	}

	/**
	 * Gets a <code>boolean</code> value
	 * Returns "true" if the retrieved value is "true", otherwise "false"
	 * Uses the {@link #getResolvedProperty(String)} method.
	 *
	 * @param key    the Key
	 * @param dfault the default value
	 * @return double
	 */
	public boolean getBoolean(String key, boolean dfault) {
		String ob = this.getResolvedProperty(key);
		if (ob == null) return dfault;

		return "true".equalsIgnoreCase(ob) || "!false".equalsIgnoreCase(ob);
	}

	/**
	 * Gets an <code>int</code> value
	 * Uses the {@link #getResolvedProperty(String)} method.
	 *
	 * @param key    the Key
	 * @param dfault the default value
	 * @return int
	 */
	public int getInt(String key, int dfault) {
		String ob = this.getResolvedProperty(key);

		if (ob == null) return dfault;
		return Integer.parseInt(ob);
	}

	/**
	 * Gets a <code>long</code> value
	 * Uses the {@link #getResolvedProperty(String)} method.
	 *
	 * @param key    the Key
	 * @param dfault the default value
	 * @return long
	 */
	public long getLong(String key, long dfault) {
		String ob = this.getResolvedProperty(key);

		if (ob == null) return dfault;
		return Long.parseLong(ob);
	}

	/**
	 * Gets a <code>double</code> value
	 * Uses the {@link #getResolvedProperty(String)} method.
	 *
	 * @param key    the Key
	 * @param dfault the default value
	 * @return double
	 */
	public double getDouble(String key, double dfault) {
		String ob = this.getResolvedProperty(key);
		if (ob == null) return dfault;
		return Double.parseDouble(ob);
	}

	/**
	 * Loads the property based on it's extension
	 */
	private synchronized void loadResource(URL url) throws IOException {
		String extension = FilenameUtils.getExtension(url.getPath());
		try (InputStream is = url.openStream(); Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(is)) {
			switch (extension) {
				case "properties":
					load(reader);
					break;
				case "yml", "yaml":
					YamlParser parser = new YamlParser();
					putAll(parser.load(reader));
					break;
				default:
					throw new IllegalArgumentException("Extension not supported: " + extension);
			}
		}
	}
}
