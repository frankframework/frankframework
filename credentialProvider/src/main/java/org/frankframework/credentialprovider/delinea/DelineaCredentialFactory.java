/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.credentialprovider.delinea;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.credentialprovider.Credentials;
import org.frankframework.credentialprovider.ICredentialFactory;
import org.frankframework.credentialprovider.ICredentials;
import org.frankframework.credentialprovider.util.CredentialConstants;

@Log4j2
public class DelineaCredentialFactory implements ICredentialFactory {

	private static final String BASE_KEY = "credentialFactory.delinea.";

	private static final String TLD_KEY = BASE_KEY + "tld";

	private static final String API_ROOT_URL_TEMPLATE_KEY = BASE_KEY + "apiRootUrlTemplate";

	private static final String OAUTH_TOKEN_URL_TEMPLATE_KEY = BASE_KEY + "oauth.tokenUrlTemplate";

	private static final String OAUTH_USERNAME_KEY = BASE_KEY + "oauth.username";

	private static final String OAUTH_PASSWORD_KEY = BASE_KEY + "oauth.password";

	// Leave empty to don't use autocomment
	static final String USE_AUTO_COMMENT_VALUE = BASE_KEY + "autocomment.value";

	private static final long CACHE_DURATION_MILLIS = 60_000L;

	private List<String> configuredAliases; // Refreshed every CACHE_DURATION_MILLIS

	private long lastFetch = 0;

	static final String TENANT_KEY = BASE_KEY + "tenant";

	static final String API_ROOT_URL_KEY = BASE_KEY + "apiRootUrl";

	static final String OAUTH_TOKEN_URL_KEY = BASE_KEY + "oauth.tokenUrl";

	private DelineaClientSettings delineaClientSettings;

	private DelineaClient delineaClient;

	@Override
	public void initialize() {
		log.info("initializing DelineaCredentialFactory");

		readConfiguration();

		if ((StringUtils.isEmpty(delineaClientSettings.apiRootUrlTemplate()) || StringUtils.isEmpty(delineaClientSettings.tokenUrlTemplate()))
				&& (StringUtils.isEmpty(delineaClientSettings.apiRootUrl()) || StringUtils.isEmpty(delineaClientSettings.oauthTokenUrl()))
				&& StringUtils.isEmpty(delineaClientSettings.tenant())) {
			throw new IllegalArgumentException("Either 'tenant' or both of 'api_root_url' and 'oauth2.token_url' or 'api_root_url_template' and 'oauth2.token_url_template' must be set.");
		}

		// For testing purposes, we need to be able to mock the client
		if (delineaClient == null) {
			this.delineaClient = new DelineaClientFactory(delineaClientSettings).getObject();
		}
	}

	private void readConfiguration() {
		CredentialConstants appConstants = CredentialConstants.getInstance();

		this.delineaClientSettings = new DelineaClientSettings(
				appConstants.get(TENANT_KEY),
				appConstants.get(API_ROOT_URL_KEY),
				appConstants.get(API_ROOT_URL_TEMPLATE_KEY),
				appConstants.get(OAUTH_TOKEN_URL_TEMPLATE_KEY),
				appConstants.get(OAUTH_TOKEN_URL_KEY),
				appConstants.get(OAUTH_USERNAME_KEY),
				appConstants.get(OAUTH_PASSWORD_KEY),
				appConstants.get(TLD_KEY),
				appConstants.get(USE_AUTO_COMMENT_VALUE)
		);
	}

	@Override
	public boolean hasCredentials(String alias) {
		return getConfiguredAliases().contains(alias);
	}

	@Override
	public Collection<String> getConfiguredAliases() {
		// use a cache for the configured aliases
		if (lastFetch + CACHE_DURATION_MILLIS > System.currentTimeMillis()) {
			return configuredAliases;
		}

		configuredAliases = delineaClient.getSecrets().stream()
				.map(Objects::toString)
				.toList();

		lastFetch = System.currentTimeMillis();

		return configuredAliases;
	}

	@Override
	public ICredentials getCredentials(String alias, Supplier<String> defaultUsernameSupplier, Supplier<String> defaultPasswordSupplier) {
		if (StringUtils.isNotEmpty(alias)) {

			// Make sure to always get a live copy of the secret
			Secret secret = delineaClient.getSecret(alias, delineaClientSettings.autoCommentValue());

			if (secret != null) {
				return translate(secret);
			}
		}

		return new Credentials(null, defaultUsernameSupplier, defaultPasswordSupplier);
	}

	void setDelineaClient(DelineaClient delineaClient) {
		this.delineaClient = delineaClient;
	}

	private Credentials translate(Secret secret) {
		String username = getFieldValue(secret, "username");
		String password = getFieldValue(secret, "password");

		return new Credentials(String.valueOf(secret.id()), () -> username, () -> password);
	}

	private String getFieldValue(Secret secret, String slugName) {
		return secret.fields().stream()
				.filter(field -> field.slug().equals(slugName))
				.map(Secret.Field::value)
				.findFirst()
				.orElse(null);
	}
}
