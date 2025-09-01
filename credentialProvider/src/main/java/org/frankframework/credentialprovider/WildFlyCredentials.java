/*
   Copyright 2022-2025 WeAreFrank!

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
import java.util.function.Consumer;

import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;

public class WildFlyCredentials extends Credentials {

	private final CredentialStore cs;

	public WildFlyCredentials(CredentialStore cs, CredentialAlias alias) {
		super(alias);
		this.cs = cs;
	}

	@Override
	protected void getCredentialsFromAlias(String usernameField, String passwordField) {
		try {
			if (aliasExists("") || aliasExists("/"+usernameField)) {
				retrieveAndSet("/"+usernameField, this::setUsername);
				retrieveAndSet("", this::setPassword);
			} else {
				throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]: alias not found");
			}
		} catch (NoSuchElementException e) {
			throw e;
		} catch (Exception e) {
			throw new NoSuchElementException("cannot obtain credentials from authentication alias [" + getAlias() + "]", e);
		}
	}

	private boolean aliasExists(String suffix) throws CredentialStoreException {
		String key=getAlias()+suffix;
		return cs.exists(key, PasswordCredential.class);
	}


	private void retrieveAndSet(String suffix, Consumer<String> setter) throws CredentialStoreException, IllegalStateException {
		String key=getAlias()+suffix;
		if (cs.exists(key, PasswordCredential.class)) {
			Password credential = cs.retrieve(key, PasswordCredential.class).getPassword();
			if (credential instanceof ClearPassword password) {
				String value = new String(password.getPassword());
				setter.accept(value);
			}
		} else {
			setter.accept(null);
		}
	}

}
