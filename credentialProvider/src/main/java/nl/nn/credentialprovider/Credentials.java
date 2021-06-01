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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

public class Credentials implements ICredentials {
	protected Logger log = LogManager.getLogger(this);

	private @Getter String alias;
	private @Setter String username;
	private @Setter String password;
	private boolean gotCredentials=false;

	public Credentials(String alias, String defaultUsername, String defaultPassword) {
		super();
		setAlias(alias);
		setUsername(defaultUsername);
		setPassword(defaultPassword);
	}


	private void getCredentials() {
		if (!gotCredentials) {
			if (StringUtils.isNotEmpty(getAlias())) {
				getCredentialsFromAlias();
			}
			gotCredentials=true;
		}
	}

	protected void getCredentialsFromAlias() {
		if (StringUtils.isEmpty(username) && StringUtils.isEmpty(password)) {
			log.warn("no credential factory for alias [{}], and no default credentials, username [{}]", alias, username);
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
