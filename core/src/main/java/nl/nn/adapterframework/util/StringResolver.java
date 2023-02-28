/*
   Copyright 2013, 2014 Nationale-Nederlanden, 2020 - 2023 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;

/**
 * Provide functionality to resolve ${property.key} to the value of the property key, recursively.
 *
 * @author Johan Verrips
 */
public class StringResolver {
	// Not allowed to use a static reference to the logger in this class.
	// Log4j2 uses StringResolver during instantiation.

	private static final String VALUE_SEPARATOR=":-";

	public static final String DELIM_START = "${";
	public static final String DELIM_STOP = "}";

	private static Collection<AdditionalStringResolver> additionalStringResolvers = null;

	/**
	 * Look up the key in the environment with {@link System#getenv(String)} and next in the System properties with
	 * {@link System#getProperty(String, String)}. The {@link SecurityException} if thrown, is hidden.
	 *
	 * @param key The key to search for.
	 * @param def The default value to return, may be {@code null}.
	 * @return the string value of the system property, or the default value if
	 *         there is no property with that key.
	 *         May return {@code null} if the default value was {@code null}.
	 *
	 * @since 1.1
	 */
	public static String getSystemProperty(String key, String def) {
		try {
			String result = System.getenv().get(key);
			if (result!=null) {
				return result;
			}
		} catch (Throwable e) {
			LogManager.getLogger(StringResolver.class).warn("Was not allowed to read environment variable [" + key + "]: "+ e.getMessage());
		}
		try {
			return System.getProperty(key, def);
		} catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			LogManager.getLogger(StringResolver.class).warn("Was not allowed to read system property [" + key + "]: " + e.getMessage());
			return def;
		}
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props1 First property object in which to find substitutions
	 * @param props2 Second property object in which to find substitutions
	 * @param propsToHide Optional collection of property names to hide from the output. If not null, then
	 *                    all credentials will also be hidden, in addition to properties named in the collection.
	 *
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide) throws IllegalArgumentException {
		return substVars(val, props1, props2, propsToHide, DELIM_START, DELIM_STOP, false);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props1 First property object in which to find substitutions
	 * @param props2 Second property object in which to find substitutions
	 * @param propsToHide Optional collection of property names to hide from the output. If not null, then
	 *                    all credentials will also be hidden, in addition to properties named in the collection.
	 * @param delimStart Start of substitution pattern delimiter
	 * @param delimStop End of substitution pattern delimiter
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide, String delimStart, String delimStop) throws IllegalArgumentException {
		return substVars(val, props1, props2, propsToHide, delimStart, delimStop, false);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props1 First property object in which to find substitutions
	 * @param props2 Second property object in which to find substitutions
	 * @param propsToHide Optional collection of property names to hide from the output. If not null, then
	 *                    all credentials will also be hidden, in addition to properties named in the collection.
	 * @param delimStart Start of substitution pattern delimiter
	 * @param delimStop End of substitution pattern delimiter
	 * @param resolveWithPropertyName Flag indicating if property names should also be part of the output, for debugging of
	 *                                configurations.
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) throws IllegalArgumentException {
		if (delimStart.equals(delimStop)) {
			throw new IllegalArgumentException("Start and End delimiters of substitution variables cannot be the same: both are '" +
				delimStart + "'");
		}

		StringBuilder sb = new StringBuilder();
		String providedDefaultValue=null;
		boolean containsDefault = false;
		int head = 0;
		int pointer, tail;
		String propertyComposer = "";

		while (true) {
			pointer = val.indexOf(delimStart, head); // index delimiter
			if (pointer == -1) { // no delimiter
				// no more variables
				if (head == 0) { // this is a simple string
					return val;
				}
				// add the tail string which contains no variables and return the result.
				sb.append(val.substring(head));
				return sb.toString();
			}
			sb.append(val, head, resolveWithPropertyName ? pointer + delimStart.length() : pointer);
			if(val.contains(VALUE_SEPARATOR)) {
				tail = val.indexOf(VALUE_SEPARATOR);
				providedDefaultValue = val.substring(tail+VALUE_SEPARATOR.length(), indexOfDelimStop(val, pointer, delimStart, delimStop));
				containsDefault=true;
			} else {
				tail = indexOfDelimStop(val, pointer, delimStart, delimStop);
			}
			if (tail == -1) {
				throw new IllegalArgumentException('[' + val + "] has no closing brace. Opening brace at position [" + pointer + "]");
			}
			String expression = val.substring(pointer, tail + delimStop.length());
			pointer += delimStart.length();
			String key = val.substring(pointer, tail);
			propertyComposer = key;
			if (key.contains(delimStart)) {
				key = substVars(key, props1, props2, resolveWithPropertyName);
				if(key.contains(VALUE_SEPARATOR) && resolveWithPropertyName) {
					propertyComposer = key;
					key = extractKeyValue(key, delimStart, delimStop, VALUE_SEPARATOR);
				}
			}

			String replacement = resolveReplacement(props1, props2, propsToHide, delimStart, delimStop, resolveWithPropertyName, key);

			if(resolveWithPropertyName) {
				sb.append(propertyComposer).append(VALUE_SEPARATOR);
			}

			if (replacement != null) {
				if (propsToHide != null && propsToHide.contains(key)) {
					replacement = StringUtil.hide(replacement);
				}
				// Do variable substitution on the replacement string
				// such that we can solve "Hello ${x1}" as "Hello p2"
				// the where the properties are
				// x1=${x2}
				// x2=p2
				if (!replacement.equals(expression) && !replacement.contains(delimStart + key + delimStop)) {
					String recursiveReplacement = substVars(replacement, props1, props2, resolveWithPropertyName);
					sb.append(recursiveReplacement);
				} else {
					sb.append(replacement);
				}
			} else {
				if(providedDefaultValue != null) { // use default value of property if missing actual
					sb.append(providedDefaultValue);
				}
			}
			if(resolveWithPropertyName) {
				sb.append(delimStop);
			}
			if(containsDefault) { // tail points to index of ':-' update tail to point delimStop
				tail = indexOfDelimStop(val, pointer, delimStart, delimStop);
			}
			head = tail + delimStop.length();
		}
	}

	private static String resolveReplacement(Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName, String key) {
		// first try in System properties
		String replacement = getSystemProperty(key, null);

		Iterator<AdditionalStringResolver> resolvers = getAdditionalStringResolvers().iterator();
		while (replacement == null && resolvers.hasNext()) {
			AdditionalStringResolver resolver = resolvers.next();
			replacement = resolver.resolve(key, props1, props2, propsToHide, delimStart, delimStop, resolveWithPropertyName);
		}
		// then try props parameter
		if (replacement == null && props1 != null) {
			replacement = getReplacementFromProps(props1, key);
		}
		if (replacement == null && props2 != null) {
			replacement = getReplacementFromProps(props2, key);
		}
		return replacement;
	}

	private static String getReplacementFromProps(Map<?, ?> props, String key) {
		if (props instanceof Properties) {
			return ((Properties) props).getProperty(key);
		} else {
			Object replacementSource = props.get(key);
			if (replacementSource != null) {
				try {
					return replacementSource instanceof StringDataSource ? ((StringDataSource)replacementSource).asString() : replacementSource.toString();
				} catch(IOException e) {
					LogManager.getLogger(StringResolver.class).error("Failed to resolve value for ["+ key +"]", e);
				}
			}
		}
		return null;
	}

	/**
	 * Resolves just the values of the properties in case a property key depends on other keys
	 * e.g. System.getProperty("prefix_${key:-value}") will find no matching data, this method extracts the 'value' for property lookup prefix_value
	 */
	private static String extractKeyValue(String key, String delimStart, String delimStop, String defaultValueSeparator) {
		StringBuilder sb = new StringBuilder();
		int pointer = 0;
		int delimStartIndex = key.indexOf(delimStart, pointer);
		if(delimStartIndex != -1) {
			sb.append(key, pointer, delimStartIndex);
			int valueSeparator = key.indexOf(defaultValueSeparator, delimStartIndex);
			if(valueSeparator != -1) {
				int delimStopIndex = indexOfDelimStop(key, delimStartIndex, delimStart, delimStop);
				String valueOfKey = key.substring(valueSeparator+defaultValueSeparator.length(), delimStopIndex);
				if(valueOfKey.contains(delimStart)) {
					sb.append(extractKeyValue(valueOfKey, delimStart, delimStop, defaultValueSeparator));
				} else {
					sb.append(valueOfKey);
				}
			}
		}
		return sb.toString();
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props1 First property object in which to find substitutions
	 * @param props2 Second property object in which to find substitutions
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2) throws IllegalArgumentException {
		return substVars(val, props1, props2, null);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props Property object in which to find substitutions
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props) throws IllegalArgumentException {
		return substVars(val, props, null);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props Property object in which to find substitutions
	 * @param resolveWithPropertyName Flag indicating if property names should also be part of the output, for debugging of
	 *                                configurations.
	 * 	@return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props, boolean resolveWithPropertyName) {
		return substVars(val, props, null, resolveWithPropertyName);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props1 First property object in which to find substitutions
	 * @param props2 Second property object in which to find substitutions
	 * @param resolveWithPropertyName Flag indicating if property names should also be part of the output, for debugging of
	 *                                configurations.
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, boolean resolveWithPropertyName) throws IllegalArgumentException {
		return substVars(val, props1, props2, null, DELIM_START, DELIM_STOP, resolveWithPropertyName);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <code><pre>
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * </pre></code> will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val Value in which to provide string substitutions
	 * @param props1 First property object in which to find substitutions
	 * @param props2 Second property object in which to find substitutions
	 * @param propsToHide Optional collection of property names to hide from the output. If not null, then
	 *                    all credentials will also be hidden, in addition to properties named in the collection.
	 * @param resolveWithPropertyName Flag indicating if property names should also be part of the output, for debugging of
	 *                                configurations.
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide, boolean resolveWithPropertyName) throws IllegalArgumentException {
		return substVars(val, props1, props2, propsToHide, DELIM_START, DELIM_STOP, resolveWithPropertyName);
	}

	/**
	 * Check if the input string needs property substitution applied.
	 * @param string String to check
	 * @return {@code true} if the input string contains the default start and end delimiters in consecutive
	 * order, otherwise {@code false}.
	 * The default delimiters are {@code "${"} and {@code "}"} respectively.
	 */
	public static boolean needsResolution(String string) {
		int j = string.indexOf(DELIM_START);
		return j>=0 && string.contains(DELIM_START) && string.indexOf(DELIM_STOP, j) >= 0;
	}

	private static int indexOfDelimStop(String val, int startPos, String delimStart, String delimStop) {
		// if variable in variable then find the correct stop delimiter
		int stopPos = startPos - delimStop.length();
		int numEmbeddedStart = 0;
		int numEmbeddedStop = 0;
		do {
			startPos += delimStart.length();
			stopPos = val.indexOf(delimStop, stopPos + delimStop.length());
			if (stopPos > 0) {
				String key = val.substring(startPos, stopPos);
				numEmbeddedStart = StringUtils.countMatches(key, delimStart);
				numEmbeddedStop = StringUtils.countMatches(key, delimStop);
			}
		} while (stopPos > 0 && numEmbeddedStart != numEmbeddedStop);
		return stopPos;
	}

	private static Collection<AdditionalStringResolver> getAdditionalStringResolvers() {
		if (additionalStringResolvers == null) {
			additionalStringResolvers = CollectionUtils.collect(AdditionalStringResolver.serviceLoader.iterator(), input -> input);
		}
		return additionalStringResolvers;
	}
}
