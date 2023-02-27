/*
   Copyright 2013, 2014 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
package nl.nn.credentialprovider.util;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;


/**
 * Provide functionality to resolve ${property.key} to the value of the property key, recursively.
 *
 * @author Johan Verrips
 */
public class StringResolver {
	// Not allowed to use a static reference to the logger in this class.
	// Log4j2 uses StringResolver during instantiation.

	public static final String DELIM_START = "${";
	public static final String DELIM_STOP = "}";

	/**
	 * Very similar to <code>System.getProperty</code> except that the
	 * {@link SecurityException} is hidden.
	 *
	 * @param key The key to search for.
	 * @param def The default value to return.
	 * @return the string value of the system property, or the default value if
	 *         there is no property with that key.
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
			Logger.getLogger(StringResolver.class.getName()).warning("Was not allowed to read environment variable [" + key + "]: "+ e.getMessage());
		}
		try {
			return System.getProperty(key, def);
		} catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			Logger.getLogger(StringResolver.class.getName()).warning("Was not allowed to read system property [" + key + "]: " + e.getMessage());
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
	 * First it looks in the System properties, if none is found and a
	 * <code>Properties</code> object is specified, it looks in the specified
	 * <code>Properties</code> object. If two <code>Properties</code> objects are
	 * specified, first it look in the first object. If none is found, it looks in
	 * the second object.
	 *
	 */
	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2) throws IllegalArgumentException {
		return substVars(val, props1, props2, DELIM_START, DELIM_STOP);
	}

	public static String substVars(String val, Map<?, ?> props1, Map<?, ?> props2, String delimStart, String delimStop) throws IllegalArgumentException {

		StringBuilder sbuf = new StringBuilder();

		int i = 0;
		int j, k;

		while (true) {
			j = val.indexOf(delimStart, i);
			if (j == -1) {
				// no more variables
				if (i == 0) { // this is a simple string
					return val;
				}
				// add the tail string which contains no variables and return the result.
				sbuf.append(val.substring(i));
				return sbuf.toString();
			}
			sbuf.append(val, i, j);
			k = indexOfDelimStop(val, j, delimStart, delimStop);
			if (k == -1) {
				throw new IllegalArgumentException('[' + val + "] has no closing brace. Opening brace at position [" + j + "]");
			}
			String expression = val.substring(j, k + delimStop.length());
			j += delimStart.length();
			String key = val.substring(j, k);
			if (key.contains(delimStart)) {
				key = substVars(key, props1, props2);
			}
			// first try in System properties
			String replacement = getSystemProperty(key, null);
			// then try props parameter
			if (replacement == null && props1 != null) {
				replacement = getReplacementFromProps(props1, key);
			}
			if (replacement == null && props2 != null) {
				replacement = getReplacementFromProps(props2, key);
			}

			if (replacement != null) {
				// Do variable substitution on the replacement string
				// such that we can solve "Hello ${x1}" as "Hello p2"
				// the where the properties are
				// x1=${x2}
				// x2=p2
				if (!replacement.equals(expression) && !replacement.contains(delimStart + key + delimStop)) {
					String recursiveReplacement = substVars(replacement, props1, props2);
					sbuf.append(recursiveReplacement);
				} else {
					sbuf.append(replacement);
				}
			}
			i = k + delimStop.length();
		}
	}

	private static String getReplacementFromProps(Map<?, ?> props, String key) {
		if (props instanceof Properties) {
			return ((Properties) props).getProperty(key);
		} else {
			Object replacementSource = props.get(key);
			if (replacementSource != null) {
				return replacementSource.toString();
			}
		}
		return null;
	}

	public static String substVars(String val, Map<?, ?> props) throws IllegalArgumentException {
		return substVars(val, props, null);
	}

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
				// Disabled embedded resolution, to avoid dependency on StringUtils
				//numEmbeddedStart = StringUtils.countMatches(key, delimStart);
				//numEmbeddedStop = StringUtils.countMatches(key, delimStop);
			}
		} while (stopPos > 0 && numEmbeddedStart != numEmbeddedStop);
		return stopPos;
	}
}
