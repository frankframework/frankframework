/*
   Copyright 2021 Nationale-Nederlanden

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

import org.apache.tomcat.util.IntrospectionUtils.PropertySource;

/**
 * PropertySource that gets its data from filesystem with secrets.
 * 
 * Example context.xml configuration attributes:
 *   user="${testiaf_user/username}"
 *   password="${testiaf_user/password}"
 *
 * Example context.xml configuration attributes with defaults:
 *   user="${testiaf_user/username:default username}"
 *   password="${testiaf_user/password:default password}"
 *
 * @author Gerrit van Brakel
 *
 */
public class CredentialProvidingPropertySource implements PropertySource{

	@Override
	public String getProperty(String key) {
		String pathElements[] = key.split("/");
		String alias = pathElements[0].trim();

		if (!CredentialFactory.hasCredential(alias)) {
			return null;
		}
		ICredentials credentials = CredentialFactory.getCredentials(alias, null, null);
		boolean returnPassword = pathElements.length==1 || pathElements[1].trim().equalsIgnoreCase("password");
		
		return returnPassword ? credentials.getPassword() : credentials.getUsername();
	}

}
