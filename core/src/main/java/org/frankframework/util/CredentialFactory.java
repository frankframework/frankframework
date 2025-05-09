/*
   Copyright 2013 Nationale-Nederlanden, 2021, 2023 WeAreFrank!

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
package org.frankframework.util;

import java.util.Collection;
import java.util.function.Supplier;

import org.frankframework.credentialprovider.Credentials;
import org.frankframework.credentialprovider.ICredentials;

/**
 * Provides user-id and password from the WebSphere authentication-alias repository.
 * A default username and password can be set, too.
 *
 * Note:
 * In WSAD the aliases are named just as you type them.
 * In WebSphere 5 and 6, and in RAD7/RSA7 aliases are prefixed with the name of the server.
 * It is therefore sensible to use a environment setting to find the name of the alias.
 *
 * @author  Gerrit van Brakel
 * @since   4.4.2
 */
public class CredentialFactory  {

	private final ICredentials credentials;

	public CredentialFactory(String alias) {
		this(alias, null, (Supplier<String>)null);
	}

	public CredentialFactory(String alias, String defaultUsername, String defaultPassword) {
		this(alias, ()->defaultUsername, ()->defaultPassword);
	}

	public CredentialFactory(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		if (alias == null) {
			credentials = new Credentials(alias, defaultUsernameSupplier, defaultPasswordSupplier);
		} else {
			credentials = org.frankframework.credentialprovider.CredentialFactory.getCredentials(alias, defaultUsernameSupplier, defaultPasswordSupplier);
		}
	}

	@Override
	public String toString() {
		return credentials.toString();
	}

	public String getAlias() {
		return credentials.getAlias();
	}

	public String getUsername() {
		return credentials.getUsername();
	}

	public String getPassword() {
		return credentials.getPassword();
	}

	public static Collection<String> getConfiguredAliases() throws Exception {
		return org.frankframework.credentialprovider.CredentialFactory.getConfiguredAliases();
	}
}
