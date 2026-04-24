package org.frankframework.credentialprovider;

import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import org.jspecify.annotations.NonNull;

import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.log4j.Log4j2;

import org.frankframework.credentialprovider.util.Cache;
import org.frankframework.credentialprovider.util.CredentialConstants;

@Log4j2
public class KubernetesCredentialFactory extends BaseKubernetesCredentialProvider {

	private final Cache<String, Secret, NoSuchElementException> configuredAliases = new Cache<>(CACHE_DURATION_MILLIS);

	@Override
	protected void postInitialize(CredentialConstants appConstants) {
		log.info("fetching secrets from Kubernetes namespace [{}]", namespace);
		List<Secret> secrets = getSecretsFromKubernetes();
		log.info("found [{}] secrets in namespace [{}]", secrets.size(), namespace);
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
			log.warn("no secrets found in namespace: {}", namespace);
		}
		return secrets;
	}
}
