/*
   Copyright 2021 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

public class MapCredentials extends Credentials {

	private final String usernameSuffix;
	private final String passwordSuffix;

	private final Map<String,String> aliases;

	public MapCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier, Map<String,String> aliases) {
		this(alias, defaultUsernameSupplier, defaultPasswordSupplier, null, null, aliases);
	}

	public MapCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier, String usernameSuffix, String passwordSuffix, Map<String,String> aliases) {
		super(alias, defaultUsernameSupplier, defaultPasswordSupplier);
		this.aliases = aliases;
		this.usernameSuffix = StringUtils.isNotEmpty(usernameSuffix) ? usernameSuffix : AbstractMapCredentialFactory.USERNAME_SUFFIX_DEFAULT;
		this.passwordSuffix = StringUtils.isNotEmpty(passwordSuffix) ? passwordSuffix : AbstractMapCredentialFactory.PASSWORD_SUFFIX_DEFAULT;
	}

	@Override
	protected void getCredentialsFromAlias() {
		if (aliases!=null) {
			String usernameKey = getAlias()+usernameSuffix;
			String passwordKey = getAlias()+passwordSuffix;
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
