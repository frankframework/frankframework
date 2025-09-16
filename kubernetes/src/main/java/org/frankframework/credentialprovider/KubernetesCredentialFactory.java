/*
   Copyright 2024-2025 WeAreFrank!

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
import java.util.Optional;

import jakarta.annotation.Nonnull;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
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
 *     <li>{@code credentialFactory.kubernetes.namespace} - the namespace from which secrets should be fetched (default value: 'default')</li>
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
 * @ff.info The credentials are cached for 60 seconds, to prevent unnecessary calls to the Kubernetes API.
 */
@Log
public class KubernetesCredentialFactory implements ISecretProvider {

	private static final String K8_USERNAME = "credentialFactory.kubernetes.username";
	private static final String K8_PASSWORD = "credentialFactory.kubernetes.password";
	static final String K8_MASTER_URL = "credentialFactory.kubernetes.masterUrl";
	private static final String K8_NAMESPACE_PROPERTY = "credentialFactory.kubernetes.namespace";

	private static final int CACHE_DURATION_MILLIS = 60_000;
	public static final String DEFAULT_NAMESPACE = "default";

	protected String namespace;
	private KubernetesClient client;

	private final Cache<String, Secret, NoSuchElementException> configuredAliases = new Cache<>(CACHE_DURATION_MILLIS);

	@Override
	public void initialize() {
		CredentialConstants appConstants = CredentialConstants.getInstance();
		log.info("initializing KubernetesCredentialFactory");
		if (client == null) { // For testing purposes
			client = new KubernetesClientBuilder().build();
		}

		String defaultNamespace = Optional.ofNullable(client.getNamespace()).orElse(DEFAULT_NAMESPACE);
		namespace = appConstants.getProperty(K8_NAMESPACE_PROPERTY, defaultNamespace);

		String k8Username = appConstants.getProperty(K8_USERNAME, null);
		String k8Passwd = appConstants.getProperty(K8_PASSWORD, null);
		String k8MasterURL = appConstants.getProperty(K8_MASTER_URL, null);
		Config config = client.getConfiguration();
		if (k8Username != null) config.setUsername(k8Username);
		if (k8Passwd != null) config.setPassword(k8Passwd);
		if (k8MasterURL != null) {
			config.setMasterUrl(k8MasterURL);
			log.info("Using Kubernetes master URL: " + k8MasterURL);
		}

		// Fetch secrets directly at startup, from Kubernetes cluster
		log.info("fetching secrets from Kubernetes namespace [" + namespace + "]");
		List<Secret> secrets = getSecretsFromKubernetes();
		log.info("found [" + secrets.size() + "] secrets in namespace [" + namespace + "]");
	}

	@Override
	public boolean hasSecret(@Nonnull CredentialAlias alias) {
		try {
			return getSecret(alias) != null;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	@Override
	public ISecret getSecret(@Nonnull CredentialAlias alias) throws NoSuchElementException {
		Secret secret = configuredAliases.computeIfAbsentOrExpired(alias.getName(), this::getSecret);

		if (secret == null) {
			throw new NoSuchElementException();
		}

		return new KubernetesSecret(alias, secret);
	}

	@Override
	public Collection<String> getConfiguredAliases() {
		return configuredAliases.keySet();
	}

	private Secret getSecret(String aliasToRetrieve) throws NoSuchElementException {
		return getSecretsFromKubernetes().stream()
				.filter(e -> aliasToRetrieve.equals(e.getMetadata().getName()))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("alias ["+aliasToRetrieve+"] not found"));
	}

	protected synchronized List<Secret> getSecretsFromKubernetes() {
		List<Secret> secrets = client.secrets().inNamespace(namespace).list().getItems();
		if (secrets.isEmpty()) {
			log.warning("no secrets found in namespace: " + namespace);
		}

		return secrets;
	}

	/** Close Kubernetes client */
	public void close() {
		client.close();
	}

	// For testing purposes
	void setClient(KubernetesClient client) {
		log.info("Setting Kubernetes client to: " + client.getClass().getName());
		this.client = client;
	}
}
