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
package nl.nn.credentialprovider;

import java.util.function.Supplier;
import java.util.logging.Logger;


import nl.nn.credentialprovider.util.StringUtil;

public class Credentials implements ICredentials {
	protected Logger log = Logger.getLogger(this.getClass().getName());

	private String alias;
	private String username;
	private String password;
	private Supplier<String> usernameSupplier;
	private Supplier<String> passwordSupplier;
	private boolean gotCredentials=false;

	public Credentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		super();
		this.alias = alias;
		usernameSupplier = defaultUsernameSupplier;
		passwordSupplier = defaultPasswordSupplier;
	}


	private void getCredentials() {
		if (!gotCredentials) {

			if (StringUtil.isNotEmpty(getAlias())) {
				try {
					getCredentialsFromAlias();
				} catch (RuntimeException e) {

					if (usernameSupplier!=null) {
						username = usernameSupplier.get();
					}
					if (passwordSupplier!=null) {
						password = passwordSupplier.get();
					}

					if (StringUtil.isEmpty(username) && StringUtil.isEmpty(password)) {
						throw e;
					}
				}
			}
			if ((username==null || username.equals("")) && usernameSupplier!=null) {
				username = usernameSupplier.get();
			}
			if ((password==null || password.equals("")) && passwordSupplier!=null) {
				password = passwordSupplier.get();
			}
			gotCredentials=true;
		}
	}

	protected void getCredentialsFromAlias() {
		if (StringUtil.isEmpty(username) && StringUtil.isEmpty(password)) {
			log.warning("no credential factory for alias ["+alias+"], and no default credentials, username ["+username+"]");
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" alias ["+getAlias()+"]");
		builder.append(" username ["+username+"]");
		return builder.toString();
	}

	public void setAlias(String string) {
		alias = string;
		gotCredentials=false;
	}
	@Override
	public String getAlias() {
		return alias;
	}

	public void setUsername(String string) {
		username = string;
	}
	@Override
	public String getUsername() {
		getCredentials();
		return username;
	}

	public void setPassword(String string) {
		password = string;
	}
	@Override
	public String getPassword() {
		getCredentials();
		return password;
	}

}
