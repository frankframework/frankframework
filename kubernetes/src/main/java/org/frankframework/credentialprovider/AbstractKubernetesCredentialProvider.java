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

import java.net.URL;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.java.Log;

import org.frankframework.credentialprovider.util.CredentialConstants;

@Log
public abstract class AbstractKubernetesCredentialProvider implements ISecretProvider {
	static final String K8_USERNAME = "credentialFactory.kubernetes.username";
	static final String K8_PASSWORD = "credentialFactory.kubernetes.password";
	static final String K8_MASTER_URL = "credentialFactory.kubernetes.masterUrl";
	static final String K8_NAMESPACE_PROPERTY = "credentialFactory.kubernetes.namespace";

	static final int CACHE_DURATION_MILLIS = 60_000;

	public static final String DEFAULT_NAMESPACE = "default";

	protected String namespace;
	protected KubernetesClient client;

	@Override
	public final void initialize() {
		CredentialConstants appConstants = CredentialConstants.getInstance();
		log.info("initializing " + getClass().getSimpleName());

		initializeClientIfNull();
		configureClient(appConstants);
		verifyConnection();

		postInitialize(appConstants);
	}

	private void initializeClientIfNull() {
		if (client == null) { // For testing purposes
			client = new KubernetesClientBuilder()
					.editOrNewConfig()
					.withConnectionTimeout(3_000)
					.withRequestTimeout(3_000)
					.withRequestRetryBackoffLimit(0)
					.endConfig()
					.build();
		}
	}

	private void configureClient(CredentialConstants appConstants) {
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
	}

	private void verifyConnection() {
		URL masterUrl = client.getMasterUrl();
		try {
			client.getKubernetesVersion();
			log.info("Connected to K8s cluster: " + masterUrl);
		} catch (Exception e) {
			throw new KubernetesClientException("unable to connect to cluster: " + masterUrl, e);
		}
	}

	protected abstract void postInitialize(CredentialConstants appConstants);

	@Override
	public boolean hasSecret(@NonNull CredentialAlias alias) {
		try {
			getSecret(alias);
			return true;
		} catch (NoSuchElementException e) {
			return false;
		}
	}

	/**
	 * A Kubernetes secret name must start and end with an alphanumeric character (RFC 1123).
	 * Allowed characters are already validated by {@link CredentialAlias}, so we only check the boundaries.
	 */
	protected boolean isAliasNameValid(CredentialAlias alias) {
		String name = alias.getName();
		if (StringUtils.isEmpty(name)) {
			return false;
		}
		return Character.isLetterOrDigit(name.charAt(0))
				&& Character.isLetterOrDigit(name.charAt(name.length() - 1));
	}

	protected void warnIfAliasNameInvalid(CredentialAlias alias) {
		if (!isAliasNameValid(alias)) {
			log.warning("A Kubernetes alias must start and end with an alphanumeric character. Given alias: " + alias.getName());
		}
	}

	public void close() {
		client.close();
	}

	// For testing purposes
	void setClient(KubernetesClient client) {
		log.info("Setting Kubernetes client to: " + client.getClass().getName());
		this.client = client;
	}
}
