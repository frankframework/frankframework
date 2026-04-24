/*
   Copyright 2026 WeAreFrank!

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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.log4j.Log4j2;

import org.frankframework.credentialprovider.util.Cache;
import org.frankframework.credentialprovider.util.CredentialConstants;

@Log4j2
public class KubernetesNamedSecretProvider extends BaseKubernetesCredentialProvider {

	static final String K8_SECRET_NAMES_PROPERTY = "credentialFactory.kubernetes.secretNames";

	private final Set<String> configuredSecretNames = new LinkedHashSet<>();

	private final Cache<String, Secret, NoSuchElementException> secretCache = new Cache<>(CACHE_DURATION_MILLIS);
	private final Cache<String, Set<String>, NoSuchElementException> configuredAliasesCache = new Cache<>(CACHE_DURATION_MILLIS);

	@Override
	protected void postInitialize(CredentialConstants appConstants) {
		loadConfiguredSecretNames(appConstants);
		log.info("configured [{}] Kubernetes named secret(s) in namespace [{}]", configuredSecretNames.size(), namespace);
	}

	private void loadConfiguredSecretNames(CredentialConstants appConstants) {
		configuredSecretNames.clear();

		String configuredNames = appConstants.getProperty(K8_SECRET_NAMES_PROPERTY, null);
		if (StringUtils.isBlank(configuredNames)) {
			throw new KubernetesClientException("no Kubernetes secret names configured; property [" + K8_SECRET_NAMES_PROPERTY + "] is empty");
		}

		Set<String> parsedNames = Arrays.stream(configuredNames.split(","))
				.map(String::trim)
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toCollection(LinkedHashSet::new));

		if (parsedNames.isEmpty()) {
			throw new KubernetesClientException("no Kubernetes secret names configured; property [" + K8_SECRET_NAMES_PROPERTY + "] is empty");
		}

		configuredSecretNames.addAll(parsedNames);
	}

	@Override
	public ISecret getSecret(@NonNull CredentialAlias alias) throws NoSuchElementException {
		warnIfAliasNameInvalid(alias);

		final String aliasName = alias.getName();
		final String prefix = aliasName + ".";

		for (String configuredSecretName : configuredSecretNames) {
			Secret secret = secretCache.computeIfAbsentOrExpired(configuredSecretName, this::getConfiguredSecretByName);
			if (secret == null || secret.getData() == null || secret.getData().isEmpty()) {
				continue;
			}

			boolean hasAnyKeyForAlias = secret.getData().keySet().stream().anyMatch(k -> k.startsWith(prefix));
			if (hasAnyKeyForAlias) {
				return new AuthAliasPrefixKubernetesSecret(alias, secret, prefix);
			}
		}

		throw new NoSuchElementException("alias [" + aliasName + "] not found in configured Kubernetes secrets " + configuredSecretNames);
	}

	@Override
	public Collection<String> getConfiguredAliases() {
		Set<String> aliases = configuredAliasesCache.computeIfAbsentOrExpired("ALL", k -> discoverAliases());
		return Collections.unmodifiableSet(aliases);
	}

	private Set<String> discoverAliases() {
		Set<String> result = new LinkedHashSet<>();

		for (String configuredSecretName : configuredSecretNames) {
			Secret secret;
			try {
				secret = secretCache.computeIfAbsentOrExpired(configuredSecretName, this::getConfiguredSecretByName);
			} catch (NoSuchElementException e) {
				continue;
			}
			if (secret == null || secret.getData() == null || secret.getData().isEmpty()) {
				continue;
			}

			for (String key : secret.getData().keySet()) {
				int dot = key.indexOf('.');
				if (dot > 0) {
					result.add(key.substring(0, dot));
				}
			}
		}

		return result;
	}

	private Secret getConfiguredSecretByName(String secretName) throws NoSuchElementException {
		Secret secret = readSecretByName(secretName);
		if (secret == null) {
			throw new NoSuchElementException("configured Kubernetes secret [" + secretName + "] not found in namespace [" + namespace + "]");
		}
		return secret;
	}

	protected synchronized Secret readSecretByName(String secretName) {
		return client.secrets()
				.inNamespace(namespace)
				.withName(secretName)
				.get();
	}
}
