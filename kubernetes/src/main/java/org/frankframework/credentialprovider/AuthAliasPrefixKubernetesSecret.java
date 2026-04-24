package org.frankframework.credentialprovider;

import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class AuthAliasPrefixKubernetesSecret extends org.frankframework.credentialprovider.Secret {

	private final Secret secret;
	private final String prefix;

	AuthAliasPrefixKubernetesSecret(CredentialAlias alias, Secret secret, String prefix) {
		super(alias);
		this.secret = secret;
		this.prefix = prefix;
	}

	@Override
	public @Nullable String getField(@Nullable String fieldname) {
		if (StringUtils.isEmpty(fieldname)) {
			return null;
		}

		String fullKey = prefix + fieldname;
		String found = secret.getData() != null ? secret.getData().get(fullKey) : null;
		if (StringUtils.isEmpty(found)) {
			log.info("no value found for alias [{}] field [{}] (fullKey [{}])", getAlias(), fieldname, fullKey);
			return null;
		}

		return new String(Base64.getDecoder().decode(found));
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
				" alias [" + getAlias() + "] prefix [" + prefix + "]";
	}
}
