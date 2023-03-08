/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.credentialprovider;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.util.AdditionalStringResolver;
import nl.nn.adapterframework.util.StringResolver;
import nl.nn.adapterframework.util.StringUtil;

/**
 * Implementation of {@link AdditionalStringResolver} for resolving user credentials using
 * the {@link CredentialFactory}.
 * This class is loaded via the ServiceLoader.
 */
public class CredentialResolver implements AdditionalStringResolver {

	public static final String CREDENTIAL_PREFIX="credential:";
	public static final String USERNAME_PREFIX="username:"; // username and password prefixes must be of same length
	public static final String PASSWORD_PREFIX="password:";

	public static final String CREDENTIAL_EXPANSION_ALLOWING_PROPERTY="authAliases.expansion.allowed"; // refers to a comma separated list of aliases for which credential expansion is allowed

	private static Set<String> authAliasesAllowedToExpand=null;

	@Override
	public String resolve(String key, Map<?, ?> props1, Map<?, ?> props2, List<String> propsToHide, String delimStart, String delimStop, boolean resolveWithPropertyName) {
		if (!key.startsWith(CREDENTIAL_PREFIX)) {
			return null;
		}
		boolean mustHideCredential = propsToHide != null;

		key = key.substring(CREDENTIAL_PREFIX.length());
		boolean username = key.startsWith(USERNAME_PREFIX);
		boolean password = key.startsWith(PASSWORD_PREFIX);
		if (username) {
			key = key.substring(USERNAME_PREFIX.length());
		} else if (password) {
			key = key.substring(PASSWORD_PREFIX.length());
		}
		String replacement;
		if (username || mayExpandAuthAlias(key, props1)) {
			String defaultValue = delimStart + key+ delimStop;
			ICredentials credentials = CredentialFactory.getCredentials(key, ()-> defaultValue, ()-> defaultValue);
			replacement = username ? credentials.getUsername() : credentials.getPassword();
		} else {
			replacement = "!!not allowed to expand credential of authAlias ["+key+"]!!";
		}
		if (mustHideCredential) {
			return StringUtil.hide(replacement);
		}

		return replacement;
	}

	private static boolean mayExpandAuthAlias(String aliasName, Map<?, ?> props1) {
		if (authAliasesAllowedToExpand==null) {
			String property = System.getProperty(CREDENTIAL_EXPANSION_ALLOWING_PROPERTY,"").trim();
			if(StringResolver.needsResolution(property)) {
				property = StringResolver.substVars(property, props1);
			}
			Set<String> aliases = new HashSet<>(Arrays.asList(property.split(",")));
			authAliasesAllowedToExpand = aliases;
		}
		return authAliasesAllowedToExpand.contains(aliasName);
	}

}
