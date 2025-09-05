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

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Properties;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

public class MapSecret extends Secret {

	private final Properties secret;

	public MapSecret(CredentialAlias alias, @Nonnull Map<String,String> aliases) {
		super(alias);

		String aliasName = alias.getName();

		secret = new Properties();
		for(Entry<String, String> entry: aliases.entrySet()) {
			String key = entry.getKey();
			if(key.startsWith(aliasName)) {
				secret.put(key, entry.getValue());
			}
		}

		if (secret.isEmpty()) {
			throw new NoSuchElementException("cannot obtain credentials from authentication alias ["+alias.getName()+"]: no aliases");
		}
	}

	@Override
	public String getField(String fieldname) throws IOException {
		if (secret.size() == 1) {
			// no field
			if(StringUtils.isBlank(fieldname)) {
				return secret.getProperty(getAlias());
			}

			// only password field
			String value = secret.getProperty("%s/%s".formatted(getAlias(), fieldname));
			if (value != null) {
				return value;
			}

			// field not found
			throw new NoSuchElementException("cannot obtain field from secret [" + getAlias() + "]");
		}

		return secret.getProperty("%s/%s".formatted(getAlias(), fieldname));
	}

}
