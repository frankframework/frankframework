/*
   Copyright 2013 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;

import org.frankframework.credentialprovider.ICredentials;

/**
 * Retries a username/password combination from the CredentialProvider.
 */
public class CredentialFactory {

	private final ICredentials credentials;
	private final String defaultUsername;
	private final String defaultPassword;

	public CredentialFactory(String alias) {
		this(alias, null, null);
	}

	public CredentialFactory(String alias, String defaultUsername, String defaultPassword) {
		credentials = org.frankframework.credentialprovider.CredentialFactory.getCredentials(alias, defaultUsername, defaultPassword);
		this.defaultUsername = defaultUsername;
		this.defaultPassword = defaultPassword;
	}

	public String getAlias() {
		return credentials.getAlias();
	}

	public String getUsername() {
		return StringUtils.defaultIfBlank(credentials.getUsername(), defaultUsername);
	}

	public String getPassword() {
		return StringUtils.defaultIfBlank(credentials.getPassword(), defaultPassword);
	}

	public static Collection<String> getConfiguredAliases() throws Exception {
		return org.frankframework.credentialprovider.CredentialFactory.getConfiguredAliases();
	}

	@Override
	public String toString() {
		return credentials.toString();
	}
}
