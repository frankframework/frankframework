/*
   Copyright 2024-2026 WeAreFrank!

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

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.jspecify.annotations.NonNull;

import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.java.Log;

import org.frankframework.credentialprovider.util.Cache;
import org.frankframework.credentialprovider.util.CredentialConstants;

/**
 * <p>CredentialFactory for Kubernetes Secret. Fetches credentials from Kubernetes secrets.</p>
 *
 * <p>The credentials are stored in Kubernetes secrets, which are base64 encoded. The keys used for the secrets are "username" and "password".</p>
 *
 * <p>The `KubernetesCredentialFactory` class uses several properties to configure its behavior. These properties are set in the
 * {@code credentialproperties.properties} file and are used to customize the connection to the Kubernetes cluster and the namespace from which secrets are
 * fetched. Here's a description of the properties:</p>
 * <ul>
 *     <li>{@code credentialFactory.kubernetes.username} - the username for authenticating with the Kubernetes cluster</li>
 *     <li>{@code credentialFactory.kubernetes.password} - the password for authenticating with the Kubernetes cluster</li>
 *     <li>{@code credentialFactory.kubernetes.masterUrl} - the master URL of the Kubernetes cluster</li>
 *     <li>{@code credentialFactory.kubernetes.namespace} - the namespace from which secrets should be fetched (default value: 'current-namespace')</li>
 * </ul>
 *
 * <p>Example configuration:</p>
 * <pre>{@code
 * credentialFactory.kubernetes.username=admin
 * credentialFactory.kubernetes.password=example-password
 * credentialFactory.kubernetes.masterUrl=http://localhost:8080
 * credentialFactory.kubernetes.namespace=my-namespace
 * }</pre>
 *
 * <p>By setting these properties, you can control how the `KubernetesCredentialFactory` interacts with the Kubernetes cluster and retrieves credentials.</p>
 *
 * <p>Adding a Kubernetes secret can be done by executing:
 * <pre>{@code
 * kubectl create secret generic db-alias-name \
 * --from-literal=username=admin --from-literal=password='example-password'
 * }</pre>
 * </p>
 *
 * @ff.info Please note that the namespace value requires to be valid according to the rules defined in
 * <a href="https://tools.ietf.org/html/rfc1123">RFC 1123</a>. This means the namespace must consist of lower case alphanumeric characters, '-' or '.',
 * and must start and end with an alphanumeric character
 * @ff.info The credentials are cached for 60 seconds, to prevent unnecessary calls to the Kubernetes API.
 */
@Log
public class KubernetesCredentialFactory extends AbstractKubernetesCredentialProvider {

	private final Cache<String, Secret, NoSuchElementException> configuredAliases = new Cache<>(CACHE_DURATION_MILLIS);

	@Override
	protected void postInitialize(CredentialConstants appConstants) {
		log.info("fetching secrets from Kubernetes namespace " + namespace);
		List<Secret> secrets = getSecretsFromKubernetes();
		log.info("found " + secrets.size() + " secrets in namespace " + namespace);
	}

	@Override
	public ISecret getSecret(@NonNull CredentialAlias alias) throws NoSuchElementException {
		warnIfAliasNameInvalid(alias);

		Secret secret = configuredAliases.computeIfAbsentOrExpired(alias.getName(), this::findSecretByName);
		if (secret == null) {
			throw new NoSuchElementException();
		}
		return new KubernetesSecret(alias, secret);
	}

	@Override
	public Collection<String> getConfiguredAliases() {
		return configuredAliases.keySet();
	}

	private Secret findSecretByName(String aliasToRetrieve) throws NoSuchElementException {
		return getSecretsFromKubernetes().stream()
				.filter(e -> aliasToRetrieve.equals(e.getMetadata().getName()))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("alias [" + aliasToRetrieve + "] not found"));
	}

	protected synchronized List<Secret> getSecretsFromKubernetes() {
		List<Secret> secrets = client.secrets().inNamespace(namespace).list().getItems();
		if (secrets.isEmpty()) {
			log.warning("no secrets found in namespace: " + namespace);
		}
		return secrets;
	}
}
