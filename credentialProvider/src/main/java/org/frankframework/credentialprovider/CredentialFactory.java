/*
   Copyright 2021 Nationale-Nederlanden, 2022-2026 WeAreFrank!

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
import java.util.Properties;
import java.util.logging.Level;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.lang.Contract;

import lombok.extern.java.Log;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.ImmutableProperties;
import org.frankframework.util.PropertyLoader;

@Log
public class CredentialFactory {

	public static final String CREDENTIAL_FACTORY_KEY = "credentialFactory.class";
	public static final String CREDENTIAL_FACTORY_ALIAS_PREFIX_KEY = "credentialFactory.optionalPrefix";

	private static final String FALLBACK_CREDENTIAL_FACTORY = FileSystemCredentialFactory.class.getName();

	public static final String LEGACY_PACKAGE_NAME = "nl.nn.credentialprovider.";
	public static final String ORG_FRANKFRAMEWORK_PACKAGE_NAME = "org.frankframework.credentialprovider.";

	public static final String DEFAULT_USERNAME_FIELD = "username";
	public static final String DEFAULT_PASSWORD_FIELD = "password";

	protected final List<ISecretProvider> delegates = new ArrayList<>();
	private static CredentialConstants properties = new CredentialConstants();

	protected static boolean ALLOW_DEFAULT_PASSWORD = properties.getBoolean("credentialFactory.allowDefaultPassword", true);
	private static final boolean ALLOW_FALLBACK = properties.getBoolean("credentialFactory.allowFallbackProvider", true);

	private static @Nullable CredentialFactory self;

	public static synchronized CredentialFactory getInstance() {
		if (self == null) {
			self = new CredentialFactory();
		}
		return self;
	}

	private static synchronized void resetProperties() {
		properties = new CredentialConstants();
	}

	/**
	 * Reset factory to its default state.
	 */
	public static synchronized void reset() {
		resetProperties();
		clearFactoryInstance();
	}

	/**
	 * Get the configuration properties of the CredentialFactory. This is a convenience method; you should never modify the properties obtained via this method.
	 */
	public static Properties getProperties() {
		return new ImmutableProperties(properties);
	}

	/**
	 * Update the value of a property. This method is for test-usage. Changing the value of any property will clear the loaded CredentialProviders, so that the
	 * changes will be reflected in any CredentialProviders accessed since.
	 *
	 * @param key Name of property to update
	 * @param value New value of the property, may not be null.
	 * @return Previous value of the property if set, otherwise null.
	 */
	public static @Nullable String setProperty(String key, String value) {
		clearFactoryInstance();
		String oldValue = properties.getProperty(key);
		properties.setProperty(key, value);
		return oldValue;
	}

	/**
	 * Get a property of the CredentialFactory, or null if not set
	 */
	public static @Nullable String getProperty(String key) {
		return properties.getProperty(key);
	}

	/**
	 * Get a property of the CredentialFactory, or defaultValue if not set
	 */
	@Contract("_, !null -> !null")
	public static @Nullable String getProperty(String key, @Nullable String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	/**
	 * Create an instance using the given {@link ISecretProvider}s, ignoring the providers specified in the configuration.
	 */
	public static synchronized void createInstance(ISecretProvider... providers) throws Exception {
		clearFactoryInstance();
		self = new CredentialFactory(providers);
	}

	private CredentialFactory() {
		this(ALLOW_FALLBACK);
	}

	// Create a non-static instance, for testing purposes only
	protected CredentialFactory(boolean allowFallbackProvider) {
		// Attempt to load the provided CredentialProviders
		if (!loadFactoryClasses()) {
			// Oops, no factories found
			if (allowFallbackProvider && !tryFactories(FALLBACK_CREDENTIAL_FACTORY)) {
				// We were allowed to try the fallback provider but it didn't work either.
				log.severe("No CredentialFactory installed");
			} else {
				// No factories found and no fallback allowed, abort!
				exit();
			}
		}
	}

	private CredentialFactory(ISecretProvider... providers) throws Exception {
		List<ISecretProvider> providerList = Arrays.asList(providers);
		if (providerList.isEmpty()) {
			throw new IllegalArgumentException("No CredentialFactory installed");
		}
		for (ISecretProvider provider : providerList) {
			provider.initialize();
			delegates.add(provider);
		}
	}

	/**
	 *
	 * @return true if we've found one or more factories
	 */
	private boolean loadFactoryClasses() {
		String factoryClassNames = getProperty(CREDENTIAL_FACTORY_KEY);

		// Legacy support for old package names; to be removed in Frank!Framework 8.1 or later
		if (StringUtils.isNotEmpty(factoryClassNames) && factoryClassNames.contains(LEGACY_PACKAGE_NAME)) {
			log.severe("please update your CredentialFactory properties to use the new namespace!");
			factoryClassNames = factoryClassNames.replace(LEGACY_PACKAGE_NAME, ORG_FRANKFRAMEWORK_PACKAGE_NAME);
		}

		// Attempt to load the provided factories
		return tryFactories(factoryClassNames);
	}

	protected void exit() {
		System.exit(12);
	}

	/**
	 * Clear the existing static instance, for testing purposes only
	 */
	private static void clearFactoryInstance() {
		self = null;
	}

	private boolean tryFactories(final @Nullable String factoryClassNames) {
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
			// Log WARN message
			log.log(Level.WARNING, () -> "Cannot instantiate CredentialFactory [" + factoryClassName + "] (" + e.getClass().getTypeName() + "): " + e.getMessage());
			// Log INFO stacktrace
			log.log(Level.INFO, e, () -> "Exception instantiating CredentialFactory [" + factoryClassName + "]");
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
	 * Entrypoint. Attempts to find the credential for the specified alias.
	 * If none is found, returns NULL, else the credential.
	 */
	@Nullable
	public static ICredentials getCredentials(@Nullable String rawAlias) {
		try {
			ICredentials credential = getCredentials(rawAlias, null, null);
			if (credential instanceof FallbackCredential) {
				log.info("no credential found, no default provided");
				return null;
			}
			return credential;
		} catch (Exception e) {
			log.log(Level.INFO, e, () -> "Cannot get credentials: " + e.getMessage());
			return null;
		}
	}

	/**
	 * Entrypoint. Attempts to find the credential for the specified alias.
	 * When none is found, uses the default (provided) fallback user/pass combination.
	 */
	public static ICredentials getCredentials(@Nullable String rawAlias, @Nullable String defaultUsername, @Nullable String defaultPassword) throws NoSuchElementException {
		final CredentialAlias alias = CredentialAlias.parse(rawAlias);
		List<ISecretProvider> credentialFactoryDelegates = getInstance().delegates;

		// If there are no delegates, return a Secret object with the default values
		if (alias == null || credentialFactoryDelegates.isEmpty()) {
			return new FallbackCredential(rawAlias, defaultUsername, defaultPassword);
		}

		for (ISecretProvider factory : credentialFactoryDelegates) {
			try {
				ISecret secret = factory.getSecret(alias);
				log.log(Level.FINE, () -> "found secret [" + secret + "] in provider [" + factory + "]");
				return readSecretFields(secret, alias);
			} catch (NoSuchElementException | IOException e) {
				// The alias was not found in this factory, continue searching
				log.log(Level.FINE, e, () -> "Alias [" + rawAlias + "] not found in credential factory [" + factory.getClass().getName() + "]: " + e.getMessage());
			}
		}

		if (!ALLOW_DEFAULT_PASSWORD) {
			throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+ rawAlias +"]: alias not found");
		}

		if (StringUtils.isNotEmpty(defaultUsername) || StringUtils.isNotEmpty(defaultPassword)) {
			log.info("no credentials found for alias [" + rawAlias + "], using default credentials");
			return new FallbackCredential(rawAlias, defaultUsername, defaultPassword);
		}

		throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+ rawAlias +"]: alias not found and no default values provided");
	}

	private static ICredentials readSecretFields(ISecret secret, CredentialAlias alias) throws IOException {
		String username;
		String password;

		try {
			username = substitute(alias.getUsernameField(), secret);
		} catch (NoSuchElementException | IOException e) {
			log.log(Level.FINE, e, () -> "username field not found in alias [" + alias + "]");
			username = null;
		}

		try {
			password = substitute(alias.getPasswordField(), secret);
		} catch (NoSuchElementException | IOException e) {
			log.log(Level.FINE, e, () -> "password field not found in alias [" + alias + "], get password from default");
			password = secret.getField("");
		}

		return new Credential(alias.getName(), username, password);
	}

	@Nullable
	private static String substitute(@Nullable String input, ISecret secret) throws IOException {
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
	@SuppressWarnings("SameParameterValue")
	private static List<String> splitWithSeparators(String str, String charsToSplitOn) {
		final char[] c = str.toCharArray();
		final List<String> list = new ArrayList<>();
		int tokenStart = 0;
		for (int pos = tokenStart + 1; pos < c.length; pos++) {
			if (charsToSplitOn.indexOf(c[pos]) == -1) {
				continue;
			}
			list.add(new String(c, tokenStart, pos - tokenStart));
			list.add(Character.toString(c[pos]));
			tokenStart = pos + 1;
		}
		if (tokenStart < c.length) {
			list.add(new String(c, tokenStart, c.length - tokenStart));
		}
		return list;
	}

	public static Collection<String> getConfiguredAliases() {
		Collection<String> aliases = new LinkedHashSet<>();
		for (ISecretProvider factory : getInstance().delegates) {
			try {
				Collection<String> configuredAliases = factory.getConfiguredAliases();
				aliases.addAll(configuredAliases);
			} catch (Exception e) {
				log.log(Level.WARNING, e, () -> "unable to find configured aliases in factory [" + factory + "]");
			}
		}
		return aliases;
	}

	static final class CredentialConstants extends PropertyLoader {

		private static final String APP_CONSTANTS_PROPERTIES_FILE = "credentialprovider.properties";

		private CredentialConstants() {
			super(Thread.currentThread().getContextClassLoader(), APP_CONSTANTS_PROPERTIES_FILE);
		}

	}
}
