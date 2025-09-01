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
package org.frankframework.credentialprovider;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.java.Log;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.StringUtil;

@Log
public class CredentialAlias {

	private static final String PROPERTY_BASE1 = "credentialFactory.map.%s";
	private static final String PROPERTY_BASE2 = "credentialFactory.ansibleVault.%s";

	private static final String USERNAME_SUFFIX = "usernameSuffix";
	private static final String PASSWORD_SUFFIX = "passwordSuffix";

	public static final String ALIAS_PREFIX;
	public static final String DEFAULT_USERNAME_FIELD;
	public static final String DEFAULT_PASSWORD_FIELD;

	@Getter
	private final String name;
	@Getter
	private final String usernameField;
	@Getter
	private final String passwordField;

	static {
		CredentialConstants constants = CredentialConstants.getInstance();
		String aliasPrefix = constants.getProperty(CredentialFactory.CREDENTIAL_FACTORY_ALIAS_PREFIX_KEY);
		if (StringUtils.isNotBlank(aliasPrefix)) {
			log.info("setting optional prefix to [" + aliasPrefix + "]");
			ALIAS_PREFIX = aliasPrefix.toLowerCase();
		} else {
			ALIAS_PREFIX = null;
		}

		String mapNameSuffix = constants.getProperty(PROPERTY_BASE1.formatted(USERNAME_SUFFIX));
		String vaultNameSuffix = constants.getProperty(PROPERTY_BASE2.formatted(USERNAME_SUFFIX));
		if (StringUtils.isNotBlank(mapNameSuffix)) {
			DEFAULT_USERNAME_FIELD = mapNameSuffix;
		} else if (StringUtils.isNotBlank(vaultNameSuffix)) {
			DEFAULT_USERNAME_FIELD = vaultNameSuffix;
		} else {
			DEFAULT_USERNAME_FIELD = CredentialFactory.DEFAULT_USERNAME_FIELD;
		}

		String mapPasswordSuffix = constants.getProperty(PROPERTY_BASE1.formatted(PASSWORD_SUFFIX));
		String vaultPasswordSuffix = constants.getProperty(PROPERTY_BASE2.formatted(PASSWORD_SUFFIX));
		if (StringUtils.isNotBlank(mapPasswordSuffix)) {
			DEFAULT_PASSWORD_FIELD = mapPasswordSuffix;
		} else if (StringUtils.isNotBlank(vaultPasswordSuffix)) {
			DEFAULT_PASSWORD_FIELD = vaultPasswordSuffix;
		} else {
			DEFAULT_PASSWORD_FIELD = CredentialFactory.DEFAULT_PASSWORD_FIELD;
		}
	}

	/**
	 * Extracting is deprecated, cleanse is not.
	 * @return NULL when empty.
	 */
	@Nonnull
	private static String extractAlias(@Nullable final String rawAlias) {
		if (StringUtils.isBlank(rawAlias)) {
			throw new IllegalArgumentException("alias may not be empty");
		}

		String aliasName = rawAlias;
		if (ALIAS_PREFIX != null && rawAlias != null && rawAlias.toLowerCase().startsWith(ALIAS_PREFIX)) {
			aliasName = rawAlias.substring(ALIAS_PREFIX.length());
		}

		String cleanAliasName = StringUtil.split(aliasName, "{").get(0);
		if (!cleanAliasName.matches("[a-zA-Z0-9.]+")) {
			throw new IllegalArgumentException("alias must only consist of letters and numbers");
		}

		return aliasName; // Return the name without the optional prefix.
	}

	private CredentialAlias(@Nullable String rawAlias) {
		String cleanAlias = extractAlias(rawAlias);

		int openParenthesis = cleanAlias.indexOf("{");
		boolean hasCloseParenthesis = cleanAlias.endsWith("}");
		if (openParenthesis > 0 && hasCloseParenthesis) {
			name = cleanAlias.substring(0, openParenthesis);

			String remainder = cleanAlias.substring(openParenthesis+1, cleanAlias.length()-1);
			String[] fieldnames = remainder.split(",");
			usernameField = StringUtils.defaultIfBlank(fieldnames[0], DEFAULT_USERNAME_FIELD);

			if (fieldnames.length == 2) {
				passwordField = StringUtils.defaultIfBlank(fieldnames[1], DEFAULT_PASSWORD_FIELD);
			} else {
				passwordField = DEFAULT_PASSWORD_FIELD;
			}
		} else if (openParenthesis > 0 || hasCloseParenthesis) {
			throw new IllegalArgumentException("'{' and '}' are special characters and must be used in tandem.");
		} else {
			name = cleanAlias;
			usernameField = DEFAULT_USERNAME_FIELD;
			passwordField = DEFAULT_PASSWORD_FIELD;
		}
	}

	@Nullable
	public static CredentialAlias parse(String rawAlias) {
		try {
			return new CredentialAlias(rawAlias);
		} catch (Exception e) {
			return null;
		}
	}
}
