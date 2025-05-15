/*
   Copyright 2021 Nationale-Nederlanden, 2022-2023 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.ClassUtils;

public class CredentialFactory {
	protected static final Logger log = Logger.getLogger(CredentialFactory.class.getName());

	private static final String CREDENTIAL_FACTORY_KEY = "credentialFactory.class";
	private static final String CREDENTIAL_FACTORY_OPTIONAL_PREFIX_KEY = "credentialFactory.optionalPrefix";
	private static final String DEFAULT_CREDENTIAL_FACTORY = FileSystemCredentialFactory.class.getName();

	public static final String LEGACY_PACKAGE_NAME = "nl.nn.credentialprovider.";
	public static final String ORG_FRANKFRAMEWORK_PACKAGE_NAME = "org.frankframework.credentialprovider.";

	private static String optionalPrefix;

	private final List<ICredentialFactory> delegates = new ArrayList<>();

	private static CredentialFactory self;

	static {
		optionalPrefix = CredentialConstants.getInstance().getProperty(CREDENTIAL_FACTORY_OPTIONAL_PREFIX_KEY);
		if (optionalPrefix != null) {
			optionalPrefix = optionalPrefix.toLowerCase();
		}
	}

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
			log.info("trying to configure CredentialFactory [" + factoryClassName + "]");
			ICredentialFactory delegate = ClassUtils.newInstance(factoryClassName, ICredentialFactory.class);
			delegate.initialize();
			log.info("installed CredentialFactory [" + factoryClassName + "]");
			delegates.add(delegate);
		} catch (Exception e) {
			log.warning("Cannot instantiate CredentialFactory [" + factoryClassName + "] (" + e.getClass().getTypeName() + "): " + e.getMessage());
		}
	}

	private static String extractAlias(final String rawAlias) {
		if (optionalPrefix != null && rawAlias != null && rawAlias.toLowerCase().startsWith(optionalPrefix)) {
			return rawAlias.substring(optionalPrefix.length());
		}
		return rawAlias;
	}

	public static boolean hasCredential(String rawAlias) {
		final String alias = extractAlias(rawAlias);
		for (ICredentialFactory factory : getInstance().delegates) {
			if (factory.hasCredentials(alias)) {
				return true;
			}
		}
		return false;
	}

	public static ICredentials getCredentials(String rawAlias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		final String alias = extractAlias(rawAlias);
		List<ICredentialFactory> credentialFactoryDelegates = getInstance().delegates;

		// If there are no delegates, return a Credentials object with the default values
		if (credentialFactoryDelegates.isEmpty()) {
			return new Credentials(alias, defaultUsernameSupplier, defaultPasswordSupplier);
		}

		for (ICredentialFactory factory : credentialFactoryDelegates) {
			try {
				ICredentials result = factory.getCredentials(alias, defaultUsernameSupplier, defaultPasswordSupplier);

				// check if the alias is the same as the one we are looking for - will throw if not
				result.getUsername();

				return result;
			} catch (NoSuchElementException e) {
				// The alias was not found in this factory, continue searching
				log.info(alias + " not found in credential factory [" + factory.getClass().getName() + "]");
			}
		}
		throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+ alias +"]: alias not found");
	}

	public static Collection<String> getConfiguredAliases() throws Exception {
		Collection<String> aliases = new LinkedHashSet<>();
		for (ICredentialFactory factory : getInstance().delegates) {
			Collection<String> configuredAliases = factory.getConfiguredAliases();
			if (configuredAliases != null) {
				aliases.addAll(configuredAliases);
			}
		}
		return aliases;
	}

}
