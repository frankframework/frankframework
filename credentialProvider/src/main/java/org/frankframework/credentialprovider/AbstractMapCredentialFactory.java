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

public abstract class AbstractMapCredentialFactory implements ICredentialProvider {

	private final String usernameSuffixProperty = getPropertyBase() + ".usernameSuffix";
	private final String passwordSuffixProperty = getPropertyBase() + ".passwordSuffix";

	static final String USERNAME_SUFFIX_DEFAULT = "/" + CredentialFactory.DEFAULT_USERNAME_FIELD;
	static final String PASSWORD_SUFFIX_DEFAULT = "/" + CredentialFactory.DEFAULT_PASSWORD_FIELD;

	private String usernameSuffix;
	private String passwordSuffix;

	private Map<String,String> aliases;

	@Override
	public void initialize() throws IOException {
		CredentialConstants appConstants = CredentialConstants.getInstance();

		aliases = getCredentialMap(appConstants);
		if (aliases == null) {
			throw new IllegalArgumentException(this.getClass().getName()+" cannot get alias map");
		}

		usernameSuffix = appConstants.getProperty(usernameSuffixProperty, USERNAME_SUFFIX_DEFAULT);
		passwordSuffix = appConstants.getProperty(passwordSuffixProperty, PASSWORD_SUFFIX_DEFAULT);
	}

	protected abstract String getPropertyBase();

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
	public boolean hasCredentials(String alias) {
		return aliases.containsKey(alias) || aliases.containsKey(alias+usernameSuffix) || aliases.containsKey(alias+passwordSuffix);
	}

	@Override
	public ICredentials getCredentials(String alias) throws NoSuchElementException {
		return new MapCredentials(alias, usernameSuffix, passwordSuffix, aliases);
	}

	@Override
	public Set<String> getConfiguredAliases() {
		Set<String> aliasNames = new LinkedHashSet<>();
		for (String name: aliases.keySet()) {
			if (name.endsWith(usernameSuffix)) {
				name = name.substring(0, name.length()-usernameSuffix.length());
			}
			if (name.endsWith(passwordSuffix)) {
				name = name.substring(0, name.length()-passwordSuffix.length());
			}
			aliasNames.add(name);
		}
		return aliasNames;
	}

}
