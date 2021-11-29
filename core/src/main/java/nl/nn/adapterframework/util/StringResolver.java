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
package nl.nn.adapterframework.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
	
	public static final String CREDENTIAL_PREFIX="credential:";
	public static final String USERNAME_PREFIX="username:"; // username and password prefixes must be of same length
	public static final String PASSWORD_PREFIX="password:";

	public static final String CREDENTIAL_EXPANSION_ALLOWING_PROPERTY="authAliases.expansion.allowed"; // refers to a comma separated list of aliases for which credential expansion is allowed
			
	private static Set<String> authAliasesAllowedToExpand=null;
	
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
			LogUtil.getLogger(StringResolver.class).warn("Was not allowed to read environment variable [" + key + "]: "+ e.getMessage());
		}
		try {
			return System.getProperty(key, def);
		} catch (Throwable e) { // MS-Java throws com.ms.security.SecurityExceptionEx
			LogUtil.getLogger(StringResolver.class).warn("Was not allowed to read system property [" + key + "]: " + e.getMessage());
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
		return substVars(val, props1, props2, propsToHide, DELIM_START, DELIM_STOP);
	}
	
	public static String substVars(String val, Map props1, Map props2, List<String> propsToHide, String delimStart, String delimStop) throws IllegalArgumentException {

		StringBuffer sbuf = new StringBuffer();

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
				sbuf.append(val.substring(i, val.length()));
				return sbuf.toString();
			}
			sbuf.append(val.substring(i, j));
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

			boolean mustHideCredential=false;
			// then check if we search for a credential
			if (replacement == null && key.startsWith(CREDENTIAL_PREFIX)) {
				mustHideCredential=true;
				key = key.substring(CREDENTIAL_PREFIX.length());
				boolean username = key.startsWith(USERNAME_PREFIX);
				boolean password = key.startsWith(PASSWORD_PREFIX);
				if (username||password) {
					key = key.substring(USERNAME_PREFIX.length()); // username and password prefixes must be of same length
				}
				if (username || mayExpandAuthAlias(key, props1)) {
					String defaultValue = delimStart + key+ delimStop;
					CredentialFactory cf = new CredentialFactory(key, defaultValue, defaultValue);
					replacement = username ? cf.getUsername() : cf.getPassword();
				} else {
					replacement = "!!not allowed to expand credential of authAlias ["+key+"]!!";
				}
			}
			
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
				if (propsToHide != null && (propsToHide.contains(key) || mustHideCredential)) {
					replacement = Misc.hide(replacement);
				}
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

	public static String substVars(String val, Map props1, Map props2) throws IllegalArgumentException {
		return substVars(val, props1, props2, null);
	}

	public static String substVars(String val, Map props) throws IllegalArgumentException {
		return substVars(val, props, null);
	}

	public static boolean needsResolution(String string) {
		int j = string.indexOf(DELIM_START);
		return j>=0 && string.indexOf(DELIM_START)>=0 && string.indexOf(DELIM_STOP, j) >= 0;
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
	
	private static boolean mayExpandAuthAlias(String aliasName, Map props1) {
		if (authAliasesAllowedToExpand==null) {
			Set<String> aliases = new HashSet<>();
			String property = System.getProperty(CREDENTIAL_EXPANSION_ALLOWING_PROPERTY,"").trim();
			if(StringResolver.needsResolution(property)) {
				property = StringResolver.substVars(property, props1);
			}
			aliases.addAll(Arrays.asList(property.split(",")));
			authAliasesAllowedToExpand = aliases;
		}
		return authAliasesAllowedToExpand.contains(aliasName);
	}
}
