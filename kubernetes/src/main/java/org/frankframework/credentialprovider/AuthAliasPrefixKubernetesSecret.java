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

import java.util.Base64;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;

import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.java.Log;

/**
 * An {@link ISecret} implementation that reads credential fields from a Kubernetes secret
 * using a dot-separated key prefix derived from the auth alias name.
 *
 * <p>Keys in the Kubernetes secret are expected to follow the convention
 * {@code <alias>.<fieldname>}, for example {@code myalias.username} or {@code myalias.password}.
 * When {@link #getField(String)} is called, the alias prefix is prepended to the field name
 * to form the full key, and the corresponding Base64-encoded value is decoded and returned.
 *
 * <p>Returns {@code null} if the field name is blank or no matching key exists in the secret.
 */
@Log
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
			log.info("no value found for alias ["+getAlias()+"] field ["+fieldname+"] (fullKey ["+fullKey+"])");
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
