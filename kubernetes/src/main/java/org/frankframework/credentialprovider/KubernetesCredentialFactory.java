/*
   Copyright 2024 WeAreFrank!

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

import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;

import org.frankframework.credentialprovider.util.CredentialConstants;

/**
 * Factory for Kubernetes Credentials. Fetches credentials from Kubernetes secrets.
 * Adding a Kubernetes secret can be done by executing:
 * kubectl create secret generic db-alias-name \
 * --from-literal=username=admin --from-literal=password='example-password'
 * It caches the secrets for a certain amount of time, to prevent unnecessary calls to the Kubernetes API.
 */
public class KubernetesCredentialFactory implements ICredentialFactory {
	protected static final Logger log = Logger.getLogger(KubernetesCredentialFactory.class.getName());
	public static final String K8_USERNAME = "credentialFactory.kubernetes.username";
	public static final String K8_PASSWORD = "credentialFactory.kubernetes.password";
	public static final String K8_MASTER_URL = "credentialFactory.kubernetes.masterUrl";
	public static final String K8_NAMESPACE_PROPERTY = "credentialFactory.kubernetes.namespace";
	public static final long CREDENTIALS_CACHE_DURATION_MILLIS = 60_000L;
	protected static final String USERNAME_KEY = "username";
	protected static final String PASSWORD_KEY = "password";
	public static final String DEFAULT_NAMESPACE = "default";

	protected String namespace;
	private KubernetesClient client;
	protected List<Credentials> credentials; // Refreshed every SECRETS_CACHE_TIMEOUT_MILLIS
	private long lastFetch = 0;

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
		log.info("Fetching secrets from Kubernetes namespace: " + namespace);
		credentials = getCredentials();
		log.info("Loaded Credential amount from Kubernetes: " + credentials.size());
	}

	@Override
	public boolean hasCredentials(String alias) {
		return getConfiguredAliases().contains(alias);
	}

	@Override
	public ICredentials getCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		if (StringUtils.isEmpty(alias)) {
			return new Credentials(null, defaultUsernameSupplier, defaultPasswordSupplier);
		}

		return getCredentials().stream()
				.filter(credential -> alias.equalsIgnoreCase(credential.getAlias()))
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("cannot obtain credentials from authentication alias [" + alias + "]: alias not found"));
	}

	@Override
	public Collection<String> getConfiguredAliases() {
		return getCredentials().stream()
				.map(Credentials::getAlias)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	protected synchronized List<Credentials> getCredentials() {
		if (lastFetch + CREDENTIALS_CACHE_DURATION_MILLIS > System.currentTimeMillis()) {
			return credentials;
		}
		List<Secret> secrets = client.secrets().inNamespace(namespace).list().getItems();
		lastFetch = System.currentTimeMillis();
		if (secrets.isEmpty()) {
			log.warning("No secrets found in namespace: " + namespace);
		}
		credentials = secrets.stream()
				.map(secret -> new Credentials(
						secret.getMetadata().getName(),
						() -> decodeFromSecret(secret, USERNAME_KEY),
						() -> decodeFromSecret(secret, PASSWORD_KEY)
				))
				.collect(Collectors.toList());
		return credentials;
	}

	protected static String decodeFromSecret(Secret secret, String key) {
		String foundKey = secret.getData().get(key);
		if (StringUtils.isEmpty(foundKey)) {
			log.info("On Credential with alias [" + secret.getMetadata().getName() + "]: No value found for key: " + key);
			return null;
		}
		return new String(Base64.getDecoder().decode(foundKey));
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

	// For testing purposes
	void clearTimer() {
		lastFetch = 0;
	}

}
