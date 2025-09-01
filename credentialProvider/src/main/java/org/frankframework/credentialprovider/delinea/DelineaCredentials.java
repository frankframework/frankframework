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
import org.frankframework.credentialprovider.ICredentials;

public class DelineaCredentials implements ICredentials {

	private final String username;
	private final String password;
	private final String alias;

	public DelineaCredentials(CredentialAlias alias, Secret secret) {
		if (secret == null) {
			throw new IllegalArgumentException();
		}

		this.alias = String.valueOf(secret.id());
		this.username = getFieldValue(secret, alias.getUsernameField());
		this.password = getFieldValue(secret, alias.getPasswordField());
	}

	private String getFieldValue(Secret secret, String slugName) {
		return secret.fields().stream()
				.filter(field -> field.slug().equals(slugName))
				.map(Secret.Field::value)
				.findFirst()
				.orElse(null);
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
}
