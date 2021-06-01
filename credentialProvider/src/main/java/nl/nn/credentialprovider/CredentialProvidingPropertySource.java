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

public class CredentialProvidingPropertySource implements PropertySource{

	@Override
	public String getProperty(String key) {
		String pathElements[] = key.split("/");
		String alias = pathElements[0].trim();

		String pathValues[] = pathElements.length==1 ? null : pathElements[1].split(":");
		boolean returnPassword = pathValues==null || pathValues[0].trim().equalsIgnoreCase("password");
		String defaultValue = pathValues!=null && pathValues.length>1 ? pathValues[1].trim() : null;

		ICredentials credentials = CredentialFactory.getCredentials(alias, returnPassword?null:defaultValue, returnPassword?defaultValue:null);
		
		return returnPassword ? credentials.getPassword() : credentials.getUsername();
	}

}
