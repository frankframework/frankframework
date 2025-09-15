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

import java.io.IOException;
import java.util.NoSuchElementException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;

public class WildFlySecret extends Secret {

	private final CredentialStore cs;

	public WildFlySecret(CredentialStore cs, CredentialAlias alias) {
		super(alias);
		this.cs = cs;

		if (!aliasExists("") && !aliasExists("/"+CredentialAlias.DEFAULT_USERNAME_FIELD)) {
			throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+getAlias()+"]: alias not found");
		}
	}

	private boolean aliasExists(String suffix) {
		String key=getAlias()+suffix;

		try {
			return cs.exists(key, PasswordCredential.class);
		} catch (CredentialStoreException e) {
			throw new NoSuchElementException("cannot obtain credentials from credential store", e);
		}
	}

	@Override
	public String getField(@Nonnull String fieldname) throws IOException {
		String key = StringUtils.isBlank(fieldname) || "password".equals(fieldname) ? getAlias() : "%s/%s".formatted(getAlias(), fieldname);
		try {
			if (cs.exists(key, PasswordCredential.class)) {
				Password credential = cs.retrieve(key, PasswordCredential.class).getPassword();
				if (credential instanceof ClearPassword password) {
					return new String(password.getPassword());
				}
			}
		} catch (CredentialStoreException | IllegalStateException e) {
			throw new IOException(e);
		}
		return null;
	}

}
