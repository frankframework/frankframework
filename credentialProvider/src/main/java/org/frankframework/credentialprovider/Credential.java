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

import org.jspecify.annotations.Nullable;

/**
 * Default credential object
 */
public class Credential implements ICredentials {
	private final @Nullable String alias;
	private final @Nullable String username;
	private final @Nullable String password;

	public Credential(@Nullable String alias, @Nullable String username, @Nullable String password) {
		this.alias = alias;
		this.username = username;
		this.password = password;
	}

	@Override
	@Nullable
	public String getAlias() {
		return alias;
	}

	@Override
	@Nullable
	public String getUsername() {
		return username;
	}

	@Override
	@Nullable
	public String getPassword() {
		return password;
	}
}
