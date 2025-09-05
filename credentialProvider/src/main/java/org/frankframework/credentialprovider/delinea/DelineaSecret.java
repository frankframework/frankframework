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
package org.frankframework.credentialprovider.delinea;

import org.frankframework.credentialprovider.CredentialAlias;
import org.frankframework.credentialprovider.Secret;

/**
 * Delinea secret implementation. Not to be confused with the DTO to represent the JSON response.
 */
public class DelineaSecret extends Secret {

	private final DelineaSecretDto secret;

	public DelineaSecret(CredentialAlias alias, DelineaSecretDto secret) {
		super(alias);

		if (!getAlias().equals(String.valueOf(secret.id()))) {
			throw new IllegalArgumentException("secret slug does not match alias ["+getAlias()+"]");
		}

		this.secret = secret;
	}

	@Override
	public String getField(String slugName) {
		return secret.fields().stream()
				.filter(field -> field.slug().equals(slugName))
				.map(DelineaSecretDto.Field::value)
				.findFirst()
				.orElse(null);
	}
}
