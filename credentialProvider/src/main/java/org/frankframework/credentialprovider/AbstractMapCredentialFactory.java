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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.credentialprovider.util.CredentialConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.StringUtil;

public abstract class AbstractMapCredentialFactory implements ISecretProvider {

	private Map<String,String> aliases;

	@Override
	public void initialize() throws IOException {
		CredentialConstants appConstants = CredentialConstants.getInstance();

		aliases = getCredentialMap(appConstants);
		if (aliases == null) {
			throw new IllegalArgumentException(this.getClass().getName()+" cannot get alias map");
		}
	}

	protected abstract Map<String,String> getCredentialMap(CredentialConstants appConstants) throws IOException;

	protected InputStream getInputStream(CredentialConstants appConstants, String key, String defaultValue, String purpose) throws IOException {
		String filename = appConstants.getProperty(key, defaultValue);
		if (StringUtils.isEmpty(filename)) {
			throw new IllegalStateException("No property ["+key+"] found for "+purpose);
		}
		try {
			return Files.newInputStream(Paths.get(filename));
		} catch (Exception e) {
			URL url = ClassUtils.getResourceURL(filename);
			if (url == null) {
				throw new FileNotFoundException("Cannot find resource ["+filename+"]");
			}
			return url.openStream();
		}
	}

	@Override
	public boolean hasSecret(CredentialAlias alias) {
		try {
			return getSecret(alias) != null;
		} catch (NoSuchElementException e) {
			return false; // if no aliases exist
		}
	}

	@Override
	public ISecret getSecret(CredentialAlias alias) throws NoSuchElementException {
		return new MapCredentials(alias, aliases);
	}

	@Override
	public Set<String> getConfiguredAliases() {
		Set<String> aliasNames = new LinkedHashSet<>();
		for (String rawName: aliases.keySet()) {
			String name = StringUtil.split(rawName, "/").get(0);
			aliasNames.add(name);
		}
		return aliasNames;
	}

}
