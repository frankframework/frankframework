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
package org.frankframework.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Provide functionality to resolve ${property.key} to the value of the property key, recursively.
 *
 * @author Johan Verrips
 */
public class StringResolver {
	public static final String DELIM_START = "${";
	// Not allowed to use a static reference to the logger in this class.
	// Log4j2 uses StringResolver during instantiation.
	public static final String DELIM_STOP = "}";
	/**
	 * Static field to access all instances via the {@link ServiceLoader}.
	 */
	private static final ServiceLoader<AdditionalStringResolver> serviceLoader = ServiceLoader.load(AdditionalStringResolver.class);
	private static final String VALUE_SEPARATOR = ":-";
	private static Collection<AdditionalStringResolver> additionalStringResolvers = null;

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val         Value in which to provide string substitutions
	 * @param props1      First property object in which to find substitutions
	 * @param props2      Second property object in which to find substitutions
	 * @param propsToHide Optional collection of property names to hide from the output. If not null, then
	 *                    all credentials will also be hidden, in addition to properties named in the collection.
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide) throws IllegalArgumentException {
		return substVars(val, props1, props2, propsToHide, DELIM_START, DELIM_STOP, false);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val         Value in which to provide string substitutions
	 * @param props1      First property object in which to find substitutions
	 * @param props2      Second property object in which to find substitutions
	 * @param propsToHide Optional collection of property names to hide from the output. If not null, then
	 *                    all credentials will also be hidden, in addition to properties named in the collection.
	 * @param delimStart  Start of substitution pattern delimiter
	 * @param delimStop   End of substitution pattern delimiter
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, String delimStart, String delimStop) throws IllegalArgumentException {
		return substVars(val, props1, props2, propsToHide, delimStart, delimStop, false);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val                     Value in which to provide string substitutions
	 * @param props1                  First property object in which to find substitutions
	 * @param props2                  Second property object in which to find substitutions
	 * @param propsToHide             Optional collection of property names to hide from the output. If not null, then
	 *                                all credentials will also be hidden, in addition to properties named in the collection.
	 * @param delimStart              Start of substitution pattern delimiter
	 * @param delimStop               End of substitution pattern delimiter
	 * @param resolveWithPropertyName Flag indicating if property names should also be part of the output, for debugging of
	 *                                configurations.
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) throws IllegalArgumentException {
		if (delimStart.equals(delimStop)) {
			throw new IllegalArgumentException("Start and End delimiters of substitution variables cannot be the same: both are '" +
					delimStart + "'");
		}

		StringBuilder sb = new StringBuilder();
		SubstitutionContext ctx = new SubstitutionContext(propsToHide, delimStart, delimStop, resolveWithPropertyName);

		while (true) {
			findNextVariable(val, ctx);
			if (ctx.isNoDelimiterFound()) { // no delimiter
				return produceFinalOutputResult(val, sb, ctx);
			}
			appendFromInput(sb, val, ctx);

			String expression = extractNextExpression(val, ctx);
			String key = extractNextKey(val, props1, props2, ctx);

			Optional<String> replacement = resolveReplacement(key, props1, props2, ctx);
			appendReplacement(sb, key, replacement, props1, props2, expression, ctx);
		}
	}

	private static String extractNextKey(String val, Map<?, ?> props1, Map<?, ?> props2, SubstitutionContext ctx) {
		String key = val.substring(ctx.pointer, ctx.tail);
		ctx.propertyComposer = key;
		if (key.contains(ctx.delimStart)) {
			key = substVars(key, props1, props2, ctx.resolveWithPropertyName);
			if (key.contains(VALUE_SEPARATOR) && ctx.resolveWithPropertyName) {
				ctx.propertyComposer = key;
				key = extractKeyValue(key, ctx);
			}
		}
		return key;
	}

	private static String extractNextExpression(String val, SubstitutionContext ctx) {
		int nextSeparatorPos = val.indexOf(VALUE_SEPARATOR, ctx.pointer);
		if (nextSeparatorPos >= 0) {
			ctx.tail = nextSeparatorPos;
			ctx.providedDefaultValue = val.substring(ctx.tail + VALUE_SEPARATOR.length(), indexOfDelimStop(val, ctx.pointer, ctx.delimStart, ctx.delimStop));
			ctx.containsDefault = true;
		} else {
			ctx.tail = indexOfDelimStop(val, ctx.pointer, ctx.delimStart, ctx.delimStop);
		}
		if (ctx.tail == -1) {
			throw new IllegalArgumentException('[' + val + "] has no closing brace. Opening brace at position [" + ctx.pointer + "]");
		}
		String expression = val.substring(ctx.pointer, ctx.tail + ctx.delimStop.length());
		ctx.pointer += ctx.delimStart.length();
		return expression;
	}

	private static void appendFromInput(StringBuilder sb, String val, SubstitutionContext ctx) {
		sb.append(val, ctx.head, ctx.resolveWithPropertyName ? ctx.pointer + ctx.delimStart.length() : ctx.pointer);
	}

	private static String produceFinalOutputResult(String val, StringBuilder sb, SubstitutionContext ctx) {
		// no more variables
		if (ctx.head == 0) { // this is a simple string
			return val;
		}
		// add the tail string which contains no variables and return the result.
		sb.append(val.substring(ctx.head));
		return sb.toString();
	}

	private static void findNextVariable(String val, SubstitutionContext ctx) {
		if (ctx.tail > 0) { // Not the first variable, so move head & tail beyond previous tail.
			if (ctx.containsDefault) { // tail points to index of ':-' update tail to point delimStop
				ctx.tail = indexOfDelimStop(val, ctx.pointer, ctx.delimStart, ctx.delimStop);
			}
			ctx.head = ctx.tail + ctx.delimStop.length();
		}
		ctx.pointer = val.indexOf(ctx.delimStart, ctx.head); // index delimiter
	}

	private static void appendReplacement(StringBuilder sb, String key, Optional<String> replacement, Map<?, ?> props1, Map<?, ?> props2, String expression, SubstitutionContext ctx) {
		if (ctx.resolveWithPropertyName) {
			sb.append(ctx.propertyComposer).append(VALUE_SEPARATOR);
		}

		if (replacement.isPresent()) {
			String replacementValue;
			if (ctx.propsToHide != null && ctx.propsToHide.contains(key)) {
				replacementValue = StringUtil.hide(replacement.get());
			} else {
				replacementValue = replacement.get();
			}
			// Do variable substitution on the replacement string
			// such that we can solve "Hello ${x1}" as "Hello p2"
			// the where the properties are
			// x1=${x2}
			// x2=p2
			if (!replacementValue.equals(expression) && !replacementValue.contains(ctx.delimStart + key + ctx.delimStop)) {
				String recursiveReplacement = substVars(replacementValue, props1, props2, ctx.resolveWithPropertyName);
				sb.append(recursiveReplacement);
			} else {
				sb.append(replacementValue);
			}
		} else {
			if (ctx.providedDefaultValue != null) { // use default value of property if missing actual
				sb.append(ctx.providedDefaultValue);
			}
		}
		if (ctx.resolveWithPropertyName) {
			sb.append(ctx.delimStop);
		}
	}

	private static Optional<String> resolveReplacement(String key, Map<?, ?> props1, Map<?, ?> props2, SubstitutionContext ctx) {
		return Environment.getSystemProperty(key, null)
				.or(() -> findInAdditionalResolvers(key, props1, props2, ctx))
				.or(() -> getReplacementFromProps(key, props1))
				.or(() -> getReplacementFromProps(key, props2));
	}

	private static Optional<String> findInAdditionalResolvers(String key, Map<?, ?> props1, Map<?, ?> props2, SubstitutionContext ctx) {
		return getAdditionalStringResolvers().stream()
				.map(resolver -> resolver.resolve(key, props1, props2, ctx.propsToHide, ctx.delimStart, ctx.delimStop, ctx.resolveWithPropertyName))
				.filter(Optional::isPresent)
				.findFirst()
				.orElse(Optional.empty());
	}

	private static Optional<String> getReplacementFromProps(String key, Map<?, ?> props) {
		if (props == null) {
			return Optional.empty();
		} else if (props instanceof Properties properties) {
			return Optional.ofNullable(properties.getProperty(key));
		} else {
			Object replacementSource = props.get(key);
			if (replacementSource != null) {
				return Optional.of(replacementSource.toString());
			}
		}
		return Optional.empty();
	}

	/**
	 * Resolves just the values of the properties in case a property key depends on other keys
	 * e.g. System.getProperty("prefix_${key:-value}") will find no matching data, this method extracts the 'value' for property lookup prefix_value
	 */
	private static String extractKeyValue(String key, SubstitutionContext ctx) {
		StringBuilder sb = new StringBuilder();
		int pointer = 0;
		int delimStartIndex = key.indexOf(ctx.delimStart, pointer);
		if (delimStartIndex != -1) {
			sb.append(key, pointer, delimStartIndex);
			int valueSeparator = key.indexOf(VALUE_SEPARATOR, delimStartIndex);
			if (valueSeparator != -1) {
				int delimStopIndex = indexOfDelimStop(key, delimStartIndex, ctx.delimStart, ctx.delimStop);
				String valueOfKey = key.substring(valueSeparator + VALUE_SEPARATOR.length(), delimStopIndex);
				if (valueOfKey.contains(ctx.delimStart)) {
					sb.append(extractKeyValue(valueOfKey, ctx));
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
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val    Value in which to provide string substitutions
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
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object.
	 *
	 * @param val   Value in which to provide string substitutions
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
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object.
	 *
	 * @param val                     Value in which to provide string substitutions
	 * @param props                   Property object in which to find substitutions
	 * @param resolveWithPropertyName Flag indicating if property names should also be part of the output, for debugging of
	 *                                configurations.
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props, boolean resolveWithPropertyName) {
		return substVars(val, props, null, resolveWithPropertyName);
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br/>
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val                     Value in which to provide string substitutions
	 * @param props1                  First property object in which to find substitutions
	 * @param props2                  Second property object in which to find substitutions
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
	 * <pre>{@code
	 * Properties prop = new Properties();
	 * prop.put("test.name", "this is a name with ${test.xx}");
	 * prop.put("test.xx", "again");
	 * System.out.println(prop.get("test.name"));
	 * }</pre>
	 * will print <code>this is a name with again</code>
	 * <p>
	 * First it looks in the System environment and System properties, if none is found
	 * then all installed {@link AdditionalStringResolver}s are scanned for providing a replacement.
	 * If no replacement is found still and a {@link Map} (or {@link Properties}) object is specified, it looks in the specified
	 * object. If two {@link Map} or {@link Properties} objects are supplied, it looks first in the first and if none found
	 * then in the second object.
	 *
	 * @param val                     Value in which to provide string substitutions
	 * @param props1                  First property object in which to find substitutions
	 * @param props2                  Second property object in which to find substitutions
	 * @param propsToHide             Optional collection of property names to hide from the output. If not null, then
	 *                                all credentials will also be hidden, in addition to properties named in the collection.
	 * @param resolveWithPropertyName Flag indicating if property names should also be part of the output, for debugging of
	 *                                configurations.
	 * @return Input string with all property reference patterns resolved to either a property value, or empty.
	 * @throws IllegalArgumentException if there were invalid input arguments.
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, Set<String> propsToHide, boolean resolveWithPropertyName) throws IllegalArgumentException {
		return substVars(val, props1, props2, propsToHide, DELIM_START, DELIM_STOP, resolveWithPropertyName);
	}

	/**
	 * Check if the input string needs property substitution applied.
	 *
	 * @param string String to check
	 * @return {@code true} if the input string contains the default start and end delimiters in consecutive
	 * 		order, otherwise {@code false}.
	 * 		The default delimiters are <pre>"${"</pre> and <pre>"}"</pre> respectively.
	 */
	public static boolean needsResolution(String string) {
		int j = string.indexOf(DELIM_START);
		return j >= 0 && string.indexOf(DELIM_STOP, j) > j;
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
			try {
				additionalStringResolvers = CollectionUtils.collect(serviceLoader.iterator(), input -> input);
			} catch (Throwable t) {
				t.printStackTrace(); //Cannot log this because it's used before Log4j2 initializes.
				additionalStringResolvers = Collections.emptyList();
			}
		}
		return additionalStringResolvers;
	}

	private static class SubstitutionContext {
		final Set<String> propsToHide;
		final String delimStart;
		final String delimStop;
		final boolean resolveWithPropertyName;
		String providedDefaultValue = null;
		boolean containsDefault = false;
		int head = 0;
		int pointer;
		int tail;
		String propertyComposer = "";

		private SubstitutionContext(Set<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
			this.propsToHide = propsToHide;
			this.delimStart = delimStart;
			this.delimStop = delimStop;
			this.resolveWithPropertyName = resolveWithPropertyName;
		}

		boolean isNoDelimiterFound() {
			return pointer == -1;
		}
	}
}
