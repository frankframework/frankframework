/*
   Copyright 2025 WeAreFrank!

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

import org.frankframework.credentialprovider.ICredentialFactory;
import org.frankframework.credentialprovider.ICredentials;
import org.frankframework.credentialprovider.util.CredentialConstants;

/**
 * <p>CredentialFactory that reads its credentials from Delinea (formerly Thycotic) Secret Server.</p>
 *
 * <p>To set up Delinea in the Framework, you need to set the following properties in {@code credentialproperties.properties}:
 *
 * <pre>{@code
 * credentialFactory.class=org.frankframework.credentialprovider.delinea.DelineaCredentialFactory
 * credentialFactory.delinea.autoComment.value=Use this comment
 * credentialFactory.delinea.tenant=waf
 * credentialFactory.delinea.tld=eu
 * credentialFactory.delinea.oauth.username=username
 * credentialFactory.delinea.oauth.password=password
 * }</pre>
 *
 * If you use these settings, the default URLs will  be used with the given properties from the code block above:
 * <ul>
 *     <li>{@code https://<tenant>.secretservercloud.<tld>/api/v1} which will translate to {@code https://waf.secretservercloud.eu/api/v1}
 *     based on these settings</li>
 *     <li>{@code https://<tenant>.secretservercloud.<tld>/oauth2/token} which will translate to {@code https://waf.secretservercloud.eu/oauth2/token}
 *     based on these settings</li>
 * </ul>
 * </p>
 *
 * <p>Please note that using the {@code credentialFactory.delinea.autoComment.value} is optional. If not set, the feature to comment before getting a secret will
 * not be used. If set, this value will be used as a comment when getting the secret. See
 * <a href="https://updates.thycotic.net/secretserver/restapiguide/TokenAuth/#tag/SecretAccessRequests/operation/SecretAccessRequestsService_CreateViewComment">
 * Delinea API documentation</a> for more information.</p>
 *
 * <p>Ideally you'd use the above, but you can also use a different url template, or specify the complete url by using one of the following properties
 * (eg: use {@code apiRootUrl} or {@code apiRootUrlTemplate}, not both. Same for the {@code tokenUrl} and {@code tokenUrlTemplate}):
 * <pre>{@code
 * # define a complete url
 * credentialFactory.delinea.apiRootUrl
 * credentialFactory.delinea.oauth.tokenUrl
 *
 * # define a custom template (make sure to use %s twice for the tenant and tld placeholders)
 * credentialFactory.delinea.apiRootUrlTemplate=https://%s.secretservercloud.%s/api/v1
 * credentialFactory.delinea.oauth.tokenUrlTemplate=https://%s.secretservercloud.%s/oauth2/token
 * }</pre>
 * </p>
 *
 * <p>Delinea secrets are referenced by <b>ID</b> in an authAlias, because they are retrieved from the Secret Server by id. See the <a
 * href="https://updates.thycotic.net/secretserver/restapiguide/TokenAuth/#tag/Secrets/operation/SecretsService_GetSecretV2">Get Secret</a> API.</p>
 *
 * <p>To use this CredentialFactory, you will have to set up a Delinea Secret Server within the Delinea Platform. In the documentation above we assume this is
 * already done and that username and password properties reference an active 'local user'</p>
 *
 * @see <a href="https://github.com/DelineaXPM/tss-sdk-java">tss-sdk-java</a> for the reference java implementation
 * @see <a href="https://updates.thycotic.net/secretserver/restapiguide/">Delinea API documentation</a>
 */
@Log4j2
public class DelineaCredentialFactory implements ICredentialFactory {

	private static final String BASE_KEY = "credentialFactory.delinea.";
	// Leave empty to not use autocomment
	static final String USE_AUTO_COMMENT_VALUE = BASE_KEY + "autocomment.value";
	static final String TENANT_KEY = BASE_KEY + "tenant";
	static final String API_ROOT_URL_KEY = BASE_KEY + "apiRootUrl";
	static final String OAUTH_TOKEN_URL_KEY = BASE_KEY + "oauth.tokenUrl";
	private static final String TLD_KEY = BASE_KEY + "tld";
	private static final String API_ROOT_URL_TEMPLATE_KEY = BASE_KEY + "apiRootUrlTemplate";
	private static final String OAUTH_TOKEN_URL_TEMPLATE_KEY = BASE_KEY + "oauth.tokenUrlTemplate";
	private static final String OAUTH_USERNAME_KEY = BASE_KEY + "oauth.username";
	private static final String OAUTH_PASSWORD_KEY = BASE_KEY + "oauth.password";
	private static final long CACHE_DURATION_MILLIS = 60_000L;
	private List<String> configuredAliases; // Refreshed every CACHE_DURATION_MILLIS
	private long lastFetch = 0;
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

		return null;
	}

	void setDelineaClient(DelineaClient delineaClient) {
		this.delineaClient = delineaClient;
	}

	private DelineaCredentials translate(Secret secret) {
		String username = getFieldValue(secret, "username");
		String password = getFieldValue(secret, "password");

		return new DelineaCredentials(String.valueOf(secret.id()), username, password);
	}

	private String getFieldValue(Secret secret, String slugName) {
		return secret.fields().stream()
				.filter(field -> field.slug().equals(slugName))
				.map(Secret.Field::value)
				.findFirst()
				.orElse(null);
	}
}
