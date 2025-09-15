/*
   Copyright 2021 Nationale-Nederlanden, 2022-2025 WeAreFrank!

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.ClassUtils;

public class CredentialFactory {
	protected static final Logger log = Logger.getLogger(CredentialFactory.class.getName());

	private static final String CREDENTIAL_FACTORY_KEY = "credentialFactory.class";
	public static final String CREDENTIAL_FACTORY_ALIAS_PREFIX_KEY = "credentialFactory.optionalPrefix";
	private static final String DEFAULT_CREDENTIAL_FACTORY = FileSystemCredentialFactory.class.getName();

	public static final String LEGACY_PACKAGE_NAME = "nl.nn.credentialprovider.";
	public static final String ORG_FRANKFRAMEWORK_PACKAGE_NAME = "org.frankframework.credentialprovider.";

	public static final String DEFAULT_USERNAME_FIELD = "username";
	public static final String DEFAULT_PASSWORD_FIELD = "password";

	protected final List<ISecretProvider> delegates = new ArrayList<>();

	private static CredentialFactory self;

	public static synchronized CredentialFactory getInstance() {
		if (self == null) {
			self = new CredentialFactory();
		}
		return self;
	}

	// Parameter is for testing purposes only
	private CredentialFactory() {
		String factoryClassNames = CredentialConstants.getInstance().getProperty(CREDENTIAL_FACTORY_KEY);

		// Legacy support for old package names; to be removed in Frank!Framework 8.1 or later
		if (StringUtils.isNotEmpty(factoryClassNames) && factoryClassNames.contains(LEGACY_PACKAGE_NAME)) {
			log.severe("please update your CredentialFactory properties to use the new namespace!");
			factoryClassNames = factoryClassNames.replace(LEGACY_PACKAGE_NAME, ORG_FRANKFRAMEWORK_PACKAGE_NAME);
		}
		if (tryFactories(factoryClassNames)) {
			return;
		}
		if (tryFactories(DEFAULT_CREDENTIAL_FACTORY)) {
			return;
		}
		log.warning("No CredentialFactory installed");
	}

	// Clear the existing static instance, for testing purposes only
	static void clearInstance() {
		self = null;
	}

	private boolean tryFactories(final String factoryClassNames) {
		if (StringUtils.isBlank(factoryClassNames)) {
			return false;
		}
		Arrays.stream(factoryClassNames.split(",")) // split on comma
				.map(String::trim) // remove leading and trailing spaces
				.forEach(this::tryFactory);
		return !delegates.isEmpty();
	}

	private void tryFactory(String factoryClassName) {
		try {
			log.info(() -> "trying to configure CredentialFactory [" + factoryClassName + "]");
			ISecretProvider delegate = ClassUtils.newInstance(factoryClassName, ISecretProvider.class);
			delegate.initialize();
			log.info(() -> "installed CredentialFactory [" + factoryClassName + "]");
			delegates.add(delegate);
		} catch (Exception e) {
			log.log(Level.WARNING, e, () -> "Cannot instantiate CredentialFactory [" + factoryClassName + "] (" + e.getClass().getTypeName() + "): " + e.getMessage());
		}
	}

	public static boolean hasCredential(String rawAlias) {
		final CredentialAlias alias = CredentialAlias.parse(rawAlias);

		if (alias != null) {
			for (ISecretProvider factory : getInstance().delegates) {
				if (factory.hasSecret(alias)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Entrypoint.
	 *
	 * Attempts to find the credential for the specified alias.
	 * If non is found, returns NULL, else the credential.
	 */
	@Nullable
	public static ICredentials getCredentials(@Nullable String rawAlias) {
		try {
			ICredentials credential = getCredentials(rawAlias, null, null);
			if (credential instanceof FallbackCredential) {
				log.fine("no credential found, no default provided");
				return null;
			}
			return credential;
		} catch (Exception e) {
			log.log(Level.FINE, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Entrypoint
	 *
	 * Attempts to find the credential for the specified alias.
	 *
	 * When non is found, uses the default (provided) fallback user/pass combination.
	 */
	@Nonnull
	public static ICredentials getCredentials(@Nullable String rawAlias, @Nullable String defaultUsername, @Nullable String defaultPassword) {
		final CredentialAlias alias = CredentialAlias.parse(rawAlias);
		List<ISecretProvider> credentialFactoryDelegates = getInstance().delegates;

		// If there are no delegates, return a Secret object with the default values
		if (alias == null || credentialFactoryDelegates.isEmpty()) {
			return new FallbackCredential(rawAlias, defaultUsername, defaultPassword);
		}

		for (ISecretProvider factory : credentialFactoryDelegates) {
			try {
				ISecret secret = factory.getSecret(alias);

				return readSecretFields(secret, alias);
			} catch (NoSuchElementException | IOException e) {
				// The alias was not found in this factory, continue searching
				log.log(Level.FINE, e, () -> rawAlias + " not found in credential factory [" + factory.getClass().getName() + "]: " + e.getMessage());
			}
		}

		if (StringUtils.isNotEmpty(defaultUsername) || StringUtils.isNotEmpty(defaultPassword)) {
			return new FallbackCredential(rawAlias, defaultUsername, defaultPassword);
		}
		throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+ rawAlias +"]: alias not found");
	}

	private static ICredentials readSecretFields(ISecret secret, CredentialAlias alias) throws IOException {
		String username = null;
		String password = null;

		try {
			username = substitute(alias.getUsernameField(), secret);
			password = substitute(alias.getPasswordField(), secret);
		} catch (NoSuchElementException | IOException ioe) {
			password = secret.getField("");
		}

		return new Credential(alias.getName(), username, password);
	}

	private static String substitute(String input, ISecret secret) throws IOException {
		if (StringUtils.isBlank(input) || StringUtils.containsNone(input, CredentialAlias.SEPARATOR_CHARACTERS)) {
			return secret.getField(input);
		}

		// Here we require some form of substitution
		List<String> usernameParts = splitWithSeparators(input, CredentialAlias.SEPARATOR_CHARACTERS);
		StringBuilder result = new StringBuilder();
		for (String part : usernameParts) {
			if (part.length() == 1 && CredentialAlias.SEPARATOR_CHARACTERS.contains(part)) {
				result.append(part);
			} else {
				String fieldValue = secret.getField(part);
				if (fieldValue != null) {
					result.append(fieldValue);
				}
			}
		}

		return result.isEmpty() ? null : result.toString();
	}

	/**
	 * String split method, includes the characters to split on.
	 * When the input is abc@def, the output will be a list ['abc', '@', 'def'].
	 */
	private static List<String> splitWithSeparators(@Nonnull String str, @Nonnull String charsToSplitOn) {
		final char[] c = str.toCharArray();
		final List<String> list = new ArrayList<>();
		int tokenStart = 0;
		for (int pos = tokenStart + 1; pos < c.length; pos++) {
			if (charsToSplitOn.indexOf(c[pos]) == -1) {
				continue;
			}
			list.add(new String(c, tokenStart, pos - tokenStart));
			list.add(Character.toString(c[pos]));
			tokenStart = pos+1;
		}
		if (tokenStart < c.length) {
			list.add(new String(c, tokenStart, c.length - tokenStart));
		}
		return list;
	}

	public static Collection<String> getConfiguredAliases() throws Exception {
		Collection<String> aliases = new LinkedHashSet<>();
		for (ISecretProvider factory : getInstance().delegates) {
			try {
				Collection<String> configuredAliases = factory.getConfiguredAliases();
				if (configuredAliases != null) {
					aliases.addAll(configuredAliases);
				}
			} catch (Exception e) {
				log.log(Level.WARNING, e, () -> "unable to find configured aliases in factory ["+factory+"]");
			}
		}
		return aliases;
	}
}
