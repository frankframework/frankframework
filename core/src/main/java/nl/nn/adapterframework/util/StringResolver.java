/*
   Copyright 2013, 2014, 2020 Nationale-Nederlanden

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

import java.util.List;
import java.util.Map;
import java.util.Properties;

import nl.nn.adapterframework.util.LogUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * Provide functionality to resolve ${property.key} to the value of the property key, recursively.
 * 
 * @author Johan Verrips 
 */
public class StringResolver {
	protected static Logger log = LogUtil.getLogger(StringResolver.class);

	private static final String DELIM_START = "${";
	private static final char DELIM_STOP = '}';
	private static final int DELIM_START_LEN = 2;
	private static final int DELIM_STOP_LEN = 1;

	public StringResolver() {
		super();
	}

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
			return System.getProperty(key, def);
		} catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			log.warn("Was not allowed to read system property [" + key + "]: " + e.getMessage());
			return def;
		}
	}

	/**
	 * Do variable substitution on a string to resolve ${x2} to the value of the
	 * property x2. This is done recursive, so that <br>
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
	public static String substVars(String val, Map props1, Map props2, List<String> propsToHide) throws IllegalArgumentException {

		StringBuffer sbuf = new StringBuffer();

		int i = 0;
		int j, k;

		while (true) {
			j = val.indexOf(DELIM_START, i);
			if (j == -1) {
				// no more variables
				if (i == 0) { // this is a simple string
					return val;
				} else { // add the tail string which contains no variables and return the result.
					sbuf.append(val.substring(i, val.length()));
					return sbuf.toString();
				}
			} else {
				sbuf.append(val.substring(i, j));
				k = indexOfDelimStop(val, j);
				if (k == -1) {
					throw new IllegalArgumentException('[' + val + "] has no closing brace. Opening brace at position [" + j + "]");
				} else {
					String expression = val.substring(j, k + DELIM_STOP_LEN);
					j += DELIM_START_LEN;
					String key = val.substring(j, k);
					if (key.contains(DELIM_START)) {
						key = substVars(key, props1, props2);
					}
					// first try in System properties
					String replacement = getSystemProperty(key, null);
					// then try props parameter
					if (replacement == null && props1 != null) {
						if (props1 instanceof Properties) {
							replacement = ((Properties) props1).getProperty(key);
						} else {
							Object replacementSource = props1.get(key);
							if (replacementSource != null) {
								replacement = replacementSource.toString();
							}
						}
					}
					if (replacement == null && props2 != null) {
						if (props2 instanceof Properties) {
							replacement = ((Properties) props2).getProperty(key);
						} else {
							Object replacementSource = props2.get(key);
							if (replacementSource != null) {
								replacement = replacementSource.toString();
							}
						}
					}

					if (replacement != null) {
						if (propsToHide != null && propsToHide.contains(key)) {
							replacement = Misc.hide(replacement);
						}
						// Do variable substitution on the replacement string
						// such that we can solve "Hello ${x1}" as "Hello p2"
						// the where the properties are
						// x1=${x2}
						// x2=p2
						if (!replacement.equals(expression)) {
							String recursiveReplacement = substVars(replacement, props1, props2);
							sbuf.append(recursiveReplacement);
						} else {
							sbuf.append(replacement);
						}
					}
					i = k + DELIM_STOP_LEN;
				}
			}
		}
	}

	public static String substVars(String val, Map props1, Map props2) throws IllegalArgumentException {
		return substVars(val, props1, props2, null);
	}

	public static String substVars(String val, Map props) throws IllegalArgumentException {
		return substVars(val, props, null);
	}

	public static boolean needsResolution(String string) {
		int j = string.indexOf(DELIM_START);
		if (j == -1) {
			return false;
		} else {
			int k = string.indexOf(DELIM_STOP, j);
			if (k == -1) {
				return false;
			} else {
				return true;
			}
		}
	}

	private static int indexOfDelimStop(String val, int startPos) {
		// if variable in variable then find the correct stop delimiter
		int stopPos = startPos - DELIM_STOP_LEN;
		int numEmbeddedStart = 0;
		int numEmbeddedStop = 0;
		do {
			startPos += DELIM_START_LEN;
			stopPos = val.indexOf(DELIM_STOP, stopPos + DELIM_STOP_LEN);
			if (stopPos > 0) {
				String key = val.substring(startPos, stopPos);
				numEmbeddedStart = StringUtils.countMatches(key, DELIM_START);
				numEmbeddedStop = StringUtils.countMatches(key, "" + DELIM_STOP);
			}
		} while (stopPos > 0 && numEmbeddedStart != numEmbeddedStop);
		return stopPos;
	}
}
