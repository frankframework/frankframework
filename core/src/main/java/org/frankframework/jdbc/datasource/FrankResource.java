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
package org.frankframework.jdbc.datasource;

import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;

import lombok.Setter;

import org.frankframework.util.AppConstants;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.StringResolver;

public class FrankResource {

	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();

	@Setter String name;
	@Setter String type;
	@Setter String url;

	// Credentials can be retrieved via `getCredentials()`.
	@Setter String authalias;
	@Setter String username;
	@Setter String password;

	// Additional connection or client settings
	@Setter Properties properties;

	public String getName() {
		if(StringUtils.isEmpty(name)) {
			throw new IllegalStateException("field [name] is required");
		}
		return StringResolver.substVars(name, APP_CONSTANTS);
	}

	public String getType() {
		return StringResolver.substVars(type, APP_CONSTANTS);
	}

	public String getUrl() {
		if(StringUtils.isEmpty(url)) {
			throw new IllegalStateException("field [url] is required");
		}
		return StringResolver.substVars(url, APP_CONSTANTS);
	}

	/**
	 * Performs a 'safe' lookup of credentials.
	 */
	public CredentialFactory getCredentials() {
		if(StringUtils.isNotEmpty(authalias)) {
			authalias = StringResolver.substVars(authalias, APP_CONSTANTS);
		}
		if(StringUtils.isNotEmpty(username)) {
			username = StringResolver.substVars(username, APP_CONSTANTS);
		}
		if(StringUtils.isNotEmpty(password)) {
			password = StringResolver.substVars(password, APP_CONSTANTS);
		}

		return new CredentialFactory(authalias, username, password);
	}

	public Properties getProperties() {
		if (properties == null) {
			return new Properties();
		}

		Properties resolvedProperties = new Properties();
		for(Entry<Object, Object> entry : properties.entrySet()) {
			String key = String.valueOf(entry.getKey());
			String value = String.valueOf(entry.getValue());
			if(StringUtils.isNotEmpty(value)) {
				resolvedProperties.setProperty(key, StringResolver.substVars(value, APP_CONSTANTS));
			}
		}
		return resolvedProperties;
	}

	@Override
	public String toString() {
		return "FrankResource ["+ name+"]";
	}
}
