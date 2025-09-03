/*
   Copyright 2021 Nationale-Nederlanden, 2021-2025 WeAreFrank!

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

import org.apache.tomcat.util.IntrospectionUtils.PropertySource;

/**
 * Tomcat PropertySource that gets its data from the configured CredentialFactory.
 *
 * Example context.xml configuration attributes:
 *   user="${testiaf_user/username}"
 *   password="${testiaf_user/password}"
 *
 * Example context.xml configuration attributes with defaults:
 *   user="${testiaf_user/username:-default username}"
 *   password="${testiaf_user/password:-default password}"
 *
 * @author Gerrit van Brakel
 *
 */
public class CredentialProvidingPropertySource implements PropertySource {

	public static final String DEFAULT_MARKER = ":";

	@Override
	public String getProperty(String key) {
		String[] keyAndDefault = key.split(DEFAULT_MARKER);

		String[] pathElements = keyAndDefault[0].split("/");
		String alias = pathElements[0].trim(); // Ignore default value in key, it will be handled by Tomcat when necessary

		try {
			if (!CredentialFactory.hasCredential(alias)) {
				return null;
			}
		} catch (NoSuchElementException e) {
			System.err.println("CredentialProvidingPropertySource: Cannot resolve alias ["+alias+"]");
			return null;
		} catch (Exception e) {
			System.err.println("CredentialProvidingPropertySource: Cannot resolve alias ["+alias+"] ("+e.getClass().getTypeName()+"): "+e.getMessage());
			return null;
		}

		boolean returnPassword = pathElements.length==1 || "password".equalsIgnoreCase(pathElements[1].trim());

		ICredentials credentials = CredentialFactory.getCredentials(alias, null, null);
		return returnPassword ? credentials.getPassword() : credentials.getUsername();
	}

}
