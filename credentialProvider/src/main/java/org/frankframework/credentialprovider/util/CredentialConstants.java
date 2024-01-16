/*
   Copyright 2023 WeAreFrank!

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
package org.frankframework.credentialprovider.util;


import org.frankframework.util.PropertyLoader;

/**
 * Singleton class that has the credential constant values for this application.<br/>
 * <p>When an instance is created, it tries to load the properties file specified
 * by the <code>propertiesFileName</code> field</p>
 * @author Niels Meijer
 * @version 3.0
 *
 */
public final class CredentialConstants extends PropertyLoader {

	private static final String APP_CONSTANTS_PROPERTIES_FILE = "credentialprovider.properties";
	private static CredentialConstants instance = null;

	private CredentialConstants() {
		super(CredentialConstants.class.getClassLoader(), APP_CONSTANTS_PROPERTIES_FILE);
	}

	public static synchronized CredentialConstants getInstance() {
		if(instance == null) {
			instance = new CredentialConstants();
		}
		return instance;
	}
}
