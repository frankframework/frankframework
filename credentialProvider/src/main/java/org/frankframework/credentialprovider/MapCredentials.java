/*
   Copyright 2021 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import java.util.Map;
import java.util.NoSuchElementException;

public class MapCredentials extends Credentials {

	private final Map<String,String> aliases;

	public MapCredentials(CredentialAlias alias, Map<String,String> aliases) {
		super(alias);
		this.aliases = aliases;
	}

	@Override
	protected void getCredentialsFromAlias(CredentialAlias alias) {
		if (aliases != null) {
			String usernameKey = "%s/%s".formatted(getAlias(), alias.getUsernameField());
			String passwordKey = "%s/%s".formatted(getAlias(), alias.getPasswordField());

			boolean foundOne = false;
			if (aliases.containsKey(usernameKey)) {
				foundOne = true;
				setUsername(aliases.get(usernameKey));
			}
			if (aliases.containsKey(passwordKey)) {
				foundOne = true;
				setPassword(aliases.get(passwordKey));
			}
			if (!foundOne && aliases.containsKey(getAlias())) {
				setPassword(aliases.get(getAlias()));
				return;
			}
			if (foundOne) {
				return;
			}
			throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]: alias not found");
		}
		throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]: no aliases");
	}

}
