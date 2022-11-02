/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.credentialprovider;

import java.util.function.Supplier;

import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.security.credential.store.CredentialStoreException;
import org.wildfly.security.password.Password;
import org.wildfly.security.password.interfaces.ClearPassword;

public class WildFlyCredentials extends Credentials {

	private CredentialStore cs;

	public WildFlyCredentials(CredentialStore cs, String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		super(alias, defaultUsernameSupplier, defaultPasswordSupplier);
		this.cs = cs;
	}

	@Override
	protected void getCredentialsFromAlias() {
		String clearPassword = null;

		try {
			if (cs.exists(getAlias()+"/username", PasswordCredential.class)) {
				Password username = cs.retrieve(getAlias(), PasswordCredential.class).getPassword();
				if (username instanceof ClearPassword) {
					clearPassword = new String(((ClearPassword) username).getPassword());
					log.info("found username [" + clearPassword + "]");
					setUsername(clearPassword);
				}
			}
			if (cs.exists(getAlias(), PasswordCredential.class)) {
				Password password = cs.retrieve(getAlias(), PasswordCredential.class).getPassword();
				if (password instanceof ClearPassword) {
					clearPassword = new String(((ClearPassword) password).getPassword());
					log.info("found password [" + clearPassword + "]");
					setPassword(clearPassword);
				}
			}
		} catch (CredentialStoreException | IllegalStateException e) {
			e.printStackTrace();
		}

	}


}
