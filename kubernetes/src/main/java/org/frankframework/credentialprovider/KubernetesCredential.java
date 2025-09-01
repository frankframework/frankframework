/*
   Copyright 2025 WeAreFrank!

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

import io.fabric8.kubernetes.api.model.Secret;
import lombok.extern.java.Log;

@Log
public class KubernetesCredential implements ICredentials {
	protected static final String USERNAME_KEY = "username";
	protected static final String PASSWORD_KEY = "password";

	private final String alias;
	private final String username;
	private final String password;

	public KubernetesCredential(Secret secret) {
		alias = secret.getMetadata().getName();
		username = decodeFromSecret(secret, USERNAME_KEY);
		password = decodeFromSecret(secret, PASSWORD_KEY);
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	protected String decodeFromSecret(Secret secret, String key) {
		String foundKey = secret.getData().get(key);
		if (StringUtils.isEmpty(foundKey)) {
			log.info("no value found for alias [%s] field [%s]".formatted(alias, key));
			return null;
		}
		return new String(Base64.getDecoder().decode(foundKey));
	}

}
