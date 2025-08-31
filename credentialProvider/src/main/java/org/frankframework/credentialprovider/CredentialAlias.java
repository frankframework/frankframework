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

import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import lombok.extern.java.Log;

import org.frankframework.credentialprovider.util.CredentialConstants;

@Log
public class CredentialAlias {

	private static final String PROPERTY_BASE1 = "credentialFactory.map.%s";
	private static final String PROPERTY_BASE2 = "credentialFactory.ansibleVault.%s";

	private static final String USERNAME_SUFFIX = "usernameSuffix";
	private static final String PASSWORD_SUFFIX = "passwordSuffix";

	public static String ALIAS_PREFIX;

	public static String DEFAULT_USERNAME_FIELD;
	public static String DEFAULT_PASSWORD_FIELD;

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
			log.severe("property [credentialFactory.optionalPrefix] should not be used!");
			ALIAS_PREFIX = aliasPrefix.toLowerCase();
		}

		String doesMapHaveNameSuffix = constants.getProperty(PROPERTY_BASE1.formatted(USERNAME_SUFFIX));
		String doesVaultHaveNameSuffix = constants.getProperty(PROPERTY_BASE2.formatted(USERNAME_SUFFIX));
		if (StringUtils.isNotBlank(doesMapHaveNameSuffix)) {
			DEFAULT_USERNAME_FIELD = doesMapHaveNameSuffix;
		} else if (StringUtils.isNotBlank(doesVaultHaveNameSuffix)) {
			DEFAULT_USERNAME_FIELD = doesVaultHaveNameSuffix;
		} else {
			DEFAULT_USERNAME_FIELD = CredentialFactory.DEFAULT_USERNAME_FIELD;
		}

		String doesMapHavePasswordSuffix = constants.getProperty(PROPERTY_BASE1.formatted(PASSWORD_SUFFIX));
		String doesVaultHavePasswordSuffix = constants.getProperty(PROPERTY_BASE2.formatted(PASSWORD_SUFFIX));
		if (StringUtils.isNotBlank(doesMapHavePasswordSuffix)) {
			DEFAULT_PASSWORD_FIELD = doesMapHavePasswordSuffix;
		} else if (StringUtils.isNotBlank(doesVaultHavePasswordSuffix)) {
			DEFAULT_PASSWORD_FIELD = doesVaultHavePasswordSuffix;
		} else {
			DEFAULT_PASSWORD_FIELD = CredentialFactory.DEFAULT_PASSWORD_FIELD;
		}
	}

	/**
	 * Extracting is deprecated, cleanse is not.
	 * @return NULL when empty.
	 */
	@Nullable
	private static String extractAlias(@Nullable final String rawAlias) {
		if (ALIAS_PREFIX != null && rawAlias != null && rawAlias.toLowerCase().startsWith(ALIAS_PREFIX)) {
			return rawAlias.substring(ALIAS_PREFIX.length());
		}
		return rawAlias;
	}

	private CredentialAlias(String rawAlias) {
		String cleanAlias = extractAlias(rawAlias);
		if (StringUtils.isBlank(rawAlias)) {
			throw new IllegalArgumentException();
		}

		if (rawAlias.contains("{") && rawAlias.contains("}")) {
			throw new IllegalArgumentException("almost supported");
		} else if (rawAlias.contains("{") || rawAlias.contains("}")) {
			throw new IllegalArgumentException("'{' and '}' are special characters and must be used in tandem.");
		} else {
			usernameField = DEFAULT_USERNAME_FIELD;
			passwordField = DEFAULT_PASSWORD_FIELD;
			name = cleanAlias;
		}
	}

	public static CredentialAlias parse(String rawAlias) {
		try {
			return new CredentialAlias(rawAlias);
		} catch (Exception e) {
			return null;
		}
	}
}
