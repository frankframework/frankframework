/*
   Copyright 2021-2025 WeAreFrank!

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
import java.util.stream.Collectors;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.StreamUtil;

/**
 * <p>CredentialFactory that reads its credentials from a plain (unencrypted) .properties file.</p>
 *
 * <p>Secret are stored in the properties file as key/value pairs, where the key is the alias and the value is the password.</p>
 *
 * @ff.info For adequate privacy in production environments, the source file should not be readable by unauthorised users.
 * @author Gerrit van Brakel
 */
public class PropertyFileCredentialFactory extends AbstractMapCredentialFactory {

	private static final String PROPERTY_BASE = "credentialFactory.map.properties";

	private static final String DEFAULT_PROPERTIES_FILE = "credentials.properties";

	@Override
	protected Map<String, String> getCredentialMap(CredentialConstants appConstants) throws IOException {
		try (InputStream propertyStream = getInputStream(appConstants, PROPERTY_BASE, DEFAULT_PROPERTIES_FILE, "Secret");
			Reader reader = StreamUtil.getCharsetDetectingInputStreamReader(propertyStream)) {
			Properties properties = new Properties();
			properties.load(reader);

			return properties.entrySet().stream()
				.collect(Collectors.toMap(
					entry -> (String) entry.getKey(),
					entry -> (String) entry.getValue(),
					(existing, replacement) -> existing, // Handle duplicate keys
					LinkedHashMap::new
				));
		}
	}
}
