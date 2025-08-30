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

/**
 * When no credential is found in any of the CredentialProviders, this may be used.
 */
public class FallbackCredential implements ICredentials {
	private final String alias;
	private final String username;
	private final String password;

	public FallbackCredential(String alias, String username, String password) {
		this.alias = alias;
		this.username = username;
		this.password = password;
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
