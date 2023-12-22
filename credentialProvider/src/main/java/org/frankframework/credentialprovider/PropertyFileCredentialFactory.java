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
package org.frankframework.credentialprovider;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.StreamUtil;

/**
 * CredentialFactory that reads its credentials from a plain (unencrypted) .properties file.
 * For adequate privacy in production environments, the source file should not be readable by unauthorised users.
 *
 * @author Gerrit van Brakel
 *
 */
public class PropertyFileCredentialFactory extends MapCredentialFactory {

	public static final String PROPERTY_BASE="credentialFactory.map";

	public static final String FILE_PROPERTY=PROPERTY_BASE+".properties";

	private static final String DEFAULT_PROPERTIES_FILE = "credentials.properties";

	@Override
	public String getPropertyBase() {
		return PROPERTY_BASE;
	}

	@Override
	protected Map<String, String> getCredentialMap(CredentialConstants appConstants) throws IOException {
		try (InputStream propertyStream = getInputStream(appConstants, FILE_PROPERTY, DEFAULT_PROPERTIES_FILE, "Credentials");
			Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(propertyStream)) {

			Properties properties = new Properties();
			properties.load(reader);
			Map<String,String> map = new LinkedHashMap<>();
			properties.forEach((k,v) -> map.put((String)k, (String)v));
			return map;
		}
	}
}
