/*
   Copyright 2021 WeAreFrank!

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
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import lombok.Setter;

public class Credentials implements ICredentials {
	protected Logger log = Logger.getLogger(this.getClass().getName());

	private String alias;
	@Setter private String username;
	@Setter private String password;
	private final Supplier<String> usernameSupplier;
	private final Supplier<String> passwordSupplier;
	private boolean hasCredentials = false;

	public Credentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		super();
		this.alias = alias;
		usernameSupplier = defaultUsernameSupplier;
		passwordSupplier = defaultPasswordSupplier;
	}

	private void getCredentials() {
		if (hasCredentials) return;

		if (StringUtils.isNotEmpty(getAlias())) {
			try {
				getCredentialsFromAlias();
			} catch (RuntimeException e) {
				if (usernameSupplier != null) {
					username = usernameSupplier.get();
				}
				if (passwordSupplier != null) {
					password = passwordSupplier.get();
				}
				if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
					throw e;
				}
			}
		}
		if ((username == null || username.isEmpty()) && usernameSupplier != null) {
			username = usernameSupplier.get();
		}
		if ((password == null || password.isEmpty()) && passwordSupplier != null) {
			password = passwordSupplier.get();
		}
		hasCredentials = true;
	}

	protected void getCredentialsFromAlias() {
		if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
			log.warning("no credential factory for alias [%s], and no default credentials, username [%s]".formatted(alias, username));
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
				" alias [" + getAlias() + "]" +
				" username [" + username + "]";
	}

	public void setAlias(String string) {
		alias = string;
		hasCredentials = false;
	}

	@Override
	public String getAlias() {
		return alias;
	}

	@Override
	public String getUsername() {
		getCredentials();
		return username;
	}

	@Override
	public String getPassword() {
		getCredentials();
		return password;
	}

}
