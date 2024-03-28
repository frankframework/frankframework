/*
   Copyright 2024 WeAreFrank!

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

import java.util.function.Supplier;

import lombok.Getter;

@Getter
public class KubernetesCredentials implements ICredentials {

	private final String alias;
	private final String username;
	private final String password;

	public KubernetesCredentials(String alias, String username, String password) {
		this.alias = alias;
		this.username = username;
		this.password = password;
	}

	public KubernetesCredentials(String alias, Supplier<String> username, Supplier<String> password) {
		this.alias = alias;
		if (username != null) {
			this.username = username.get();
		} else this.username = null;
		if (password != null) {
			this.password = password.get();
		} else this.password = null;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
				" alias [" + getAlias() + "]" +
				" username [" + username + "]";
	}

}
