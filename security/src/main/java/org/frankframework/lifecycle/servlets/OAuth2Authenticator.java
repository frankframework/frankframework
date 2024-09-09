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
package org.frankframework.lifecycle.servlets;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistration.Builder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.SecurityFilterChain;

import lombok.Setter;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;

/**
 * OAuth2 Authentication provider which contains 4 defaults (Google, GitHub,
 * Facebook and Okta), as well as a custom setting which allows users to
 * use their own IDP.
 *
 * <p>
 * Default redirect url is as follows:
 * <pre>{@code
 * {baseUrl}/-servlet-name-/oauth2/code/{registrationId}
 * }</pre>
 * <p>
 * The redirect url has been modified to match the servlet path and is deduced from the default
 * {@link OAuth2LoginAuthenticationFilter#DEFAULT_FILTER_PROCESSES_URI}.
 * Authentication base URL: -servlet-name- {@value OAuth2AuthorizationRequestRedirectFilter#DEFAULT_AUTHORIZATION_REQUEST_BASE_URI}
 *
 * @author Niels Meijer
 *
 */
public class OAuth2Authenticator extends ServletAuthenticatorBase {

	/** eg. openid, profile, email */
	private @Setter String scopes;

	/** eg. https://accounts.google.com/o/oauth2/v2/auth */
	private @Setter String authorizationUri;

	/** eg. https://www.googleapis.com/oauth2/v4/token */
	private @Setter String tokenUri;

	/** eg. https://www.googleapis.com/oauth2/v3/certs */
	private @Setter String jwkSetUri;

	/** eg. https://accounts.google.com */
	private @Setter String issuerUri;

	/** eg. https://www.googleapis.com/oauth2/v3/userinfo */
	private @Setter String userInfoUri;

	/** eg. {@value IdTokenClaimNames#SUB} */
	private @Setter String userNameAttributeName;

	private @Setter String clientId = null;
	private @Setter String clientSecret = null;

	/** Google, GitHub, Facebook, Okta, Custom */
	private @Setter String provider;

	private ClientRegistrationRepository clientRepository;
	private String oauthBaseUrl;

	private @Setter String roleMappingFile = "oauth-role-mapping.properties";
	private URL roleMappingURL = null;

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		configure();

		AuthorityMapper authorityMapper = new AuthorityMapper(roleMappingURL, getSecurityRoles(), getEnvironmentProperties());
		http.oauth2Login(login -> login
				.clientRegistrationRepository(clientRepository) //explicitly set, but can also be implicitly implied.
				.authorizedClientService(new InMemoryOAuth2AuthorizedClientService(clientRepository))
				.failureUrl(oauthBaseUrl + "/oauth2/failure/")
				.authorizationEndpoint(endpoint -> endpoint.baseUri(oauthBaseUrl + OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI))
				.userInfoEndpoint(endpoint -> endpoint.userAuthoritiesMapper(authorityMapper))
				.loginProcessingUrl(oauthBaseUrl + "/oauth2/code/*"));

		return http.build();
	}

	private void configure() throws FileNotFoundException {
		if(StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
			throw new IllegalStateException("clientId and clientSecret must be set");
		}

		roleMappingURL = ClassUtils.getResourceURL(roleMappingFile);
		if(roleMappingURL == null) {
			throw new FileNotFoundException("unable to find OAUTH role-mapping file ["+roleMappingFile+"]");
		}
		log.info("found rolemapping file [{}]", roleMappingURL);

		oauthBaseUrl = computeBaseUrl();
		clientRepository = createClientRegistrationRepository();
		SpringUtils.registerSingleton(getApplicationContext(), "clientRegistrationRepository", clientRepository);
	}

	public ClientRegistrationRepository createClientRegistrationRepository() {
		Stream<String> providers = StringUtil.splitToStream(provider);
		List<ClientRegistration> registrations = providers.map(this::getRegistration).collect(Collectors.toList());

		return new InMemoryClientRegistrationRepository(registrations);
	}

	private ClientRegistration getRegistration(String provider) {
		ClientRegistration.Builder builder;

		switch (provider.toLowerCase()) {
			case "google":
			case "github":
			case "facebook":
			case "okta":
				CommonOAuth2Provider commonProvider = EnumUtils.parse(CommonOAuth2Provider.class, provider);
				builder = commonProvider.getBuilder(provider);
				break;

			case "custom":
				builder = createCustomBuilder(provider, provider.toLowerCase());
				break;

			default:
				throw new IllegalStateException("unknown OAuth provider");
		}

		builder.clientId(clientId).clientSecret(clientSecret);
		builder.redirectUri("{baseUrl}/%s/oauth2/code/{registrationId}".formatted(oauthBaseUrl));

		return builder.build();
	}

	public Builder createCustomBuilder(String name, String registrationId) {
		ClientRegistration.Builder builder = ClientRegistration.withRegistrationId(registrationId);
		builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
		builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);

		builder.scope(StringUtil.split(scopes));
		builder.authorizationUri(authorizationUri);
		builder.tokenUri(tokenUri);
		builder.jwkSetUri(jwkSetUri);
		builder.issuerUri(issuerUri);
		builder.userInfoUri(userInfoUri);
		builder.userNameAttributeName(userNameAttributeName);
		builder.clientName(name);

		return builder;
	}

	private String computeBaseUrl() {
		String baseUrl = getPrivateEndpoints().stream().findFirst().orElse("");
		if(baseUrl.endsWith("*")) { //Strip the '*' if the url ends with it
			baseUrl = baseUrl.substring(0, baseUrl.length()-1);
		}
		if(baseUrl.endsWith("/")) { //Ensure the url does not end with a slash
			baseUrl = baseUrl.substring(0, baseUrl.length()-1);
		}

		return baseUrl;
	}
}
