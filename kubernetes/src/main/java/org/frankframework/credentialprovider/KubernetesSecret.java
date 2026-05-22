/*
   Copyright 2025-2026 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.java.Log;

/**
 * An {@link ISecret} implementation backed by a single Kubernetes secret.
 *
 * <p>The Kubernetes secret name must match the auth alias name exactly. Fields are looked up
 * directly by key (e.g. {@code username}, {@code password}), and the Base64-encoded values
 * stored by Kubernetes are decoded before being returned.
 *
 * <p>Returns {@code null} if the requested field key is not present in the secret.
 *
 * @see KubernetesCredentialFactory
 */
@NullMarked
@Log
public class KubernetesSecret extends org.frankframework.credentialprovider.Secret {

	private final Secret secret;

	public KubernetesSecret(CredentialAlias alias, Secret secret) {
		super(alias);

		String secretAlias = secret.getMetadata().getName();
		if (!getAlias().equals(secretAlias)) {
			throw new IllegalStateException("alias does not match secret");
		}

		this.secret = secret;
	}

	@Override
	public String getField(@Nullable String key) {
		String foundKey = secret.getData().get(key);
		if (StringUtils.isEmpty(foundKey)) {
			log.info("no value found for alias [" + getAlias() + "] and field " + key);
			return null;
		}
		return new String(Base64.getDecoder().decode(foundKey));
	}
}
