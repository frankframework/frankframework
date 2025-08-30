/*
   Copyright 2021-2025 WeAreFrank!

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

import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;

import lombok.Setter;
import lombok.extern.java.Log;

@Log
public abstract class Credentials implements ICredentials {

	private String alias;
	@Setter private String username;
	@Setter private String password;
	private boolean hasCredentials = false;

	public Credentials(String alias) {
		// logic
		if (StringUtils.isBlank(alias)) {
			throw new IllegalArgumentException("no alias provided");
		}

		this.alias = alias;
	}

	private void getCredentials() {
		if (hasCredentials) return;

		if (StringUtils.isNotEmpty(getAlias())) {
			try {
				getCredentialsFromAlias();
			} catch (NoSuchElementException e) {
				log.fine("unable to find alias [%s]".formatted(alias));
				throw e;
			} catch (RuntimeException e) {
				log.warning("unable to find alias [%s]".formatted(alias));
				throw e;
			}
		}
		hasCredentials = true;
	}

	abstract void getCredentialsFromAlias();

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
