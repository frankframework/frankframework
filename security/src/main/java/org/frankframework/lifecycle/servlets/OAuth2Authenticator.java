/*
   Copyright 2023-2025 WeAreFrank!

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

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

import lombok.Getter;
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
 * {baseUrl} resolves to {baseScheme}://{baseHost}{basePort}{basePath}.
 * 
 * The redirect url has been modified to match the servlet path and is deduced from the default
 * {@link OAuth2LoginAuthenticationFilter#DEFAULT_FILTER_PROCESSES_URI}.
 * Authentication base URL is: `-servlet-name-/oauth2/authorization`
 * 
 * See https://docs.spring.io/spring-security/reference/servlet/oauth2/client/authorization-grants.html#oauth2Client-auth-code-redirect-uri
 * Default oauth2 path: `OAuth2AuthorizationRequestRedirectFilter#DEFAULT_AUTHORIZATION_REQUEST_BASE_URI`
 *
 * @author Niels Meijer
 *
 */
public class OAuth2Authenticator extends AbstractServletAuthenticator {

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

	/** eg. external absolute URL (must start with `http(s)://`) */
	private @Setter String baseUrl;

	/** eg. https://www.googleapis.com/oauth2/v3/userinfo */
	private @Setter String userInfoUri;

	/** eg. {@value IdTokenClaimNames#SUB} */
	private @Setter String userNameAttributeName;

	private @Setter String clientId = null;
	private @Setter String clientSecret = null;

	/** Only used in combination with Azure */
	private @Setter String tenantId = null;

	/** Google, GitHub, Facebook, Okta, Azure, Custom */
	private @Setter String provider;

	private ClientRegistrationRepository clientRepository;
	private String servletPath;
	private @Getter String redirectUri;

	private @Setter String roleMappingFile = "oauth-role-mapping.properties";
	private URL roleMappingURL = null;

	@Override
	public SecurityFilterChain configure(HttpSecurity http) throws Exception {
		configure();

		AuthorityMapper authorityMapper = new AuthorityMapper(roleMappingURL, getSecurityRoles(), getEnvironmentProperties());

		// The 3 dynamic URLs use the servlet path, this cannot be changed or contain {baseUrl}.
		http.oauth2Login(login -> login
				.clientRegistrationRepository(clientRepository) // Explicitly set, but can also be implicitly implied.
				.authorizedClientService(new InMemoryOAuth2AuthorizedClientService(clientRepository))
				.failureUrl(servletPath + "/oauth2/failure/")
				.authorizationEndpoint(endpoint -> endpoint.baseUri(servletPath + OAuth2AuthorizationRequestRedirectFilter.DEFAULT_AUTHORIZATION_REQUEST_BASE_URI))
				.userInfoEndpoint(endpoint -> endpoint.userAuthoritiesMapper(authorityMapper))
				.loginProcessingUrl(servletPath + "/oauth2/code/*"));

		return http.build();
	}

	private void configure() throws FileNotFoundException {
		if (StringUtils.isEmpty(clientId) || StringUtils.isEmpty(clientSecret)) {
			throw new IllegalStateException("clientId and clientSecret must be set");
		}
		if (StringUtils.isEmpty(provider)) {
			throw new IllegalStateException("A provider must be set");
		}

		roleMappingURL = ClassUtils.getResourceURL(roleMappingFile);
		if(roleMappingURL == null) {
			throw new FileNotFoundException("unable to find OAUTH role-mapping file ["+roleMappingFile+"]");
		}
		log.info("found rolemapping file [{}]", roleMappingURL);

		servletPath = computeRelativePathFromServlet();
		redirectUri = computeRedirectUri();
		log.debug("using oauth servlet-path [{}] and redirect-uri [{}]", servletPath, redirectUri);

		clientRepository = createClientRegistrationRepository();
		SpringUtils.registerSingleton(getApplicationContext(), "clientRegistrationRepository", clientRepository);
	}

	public ClientRegistrationRepository createClientRegistrationRepository() {
		return new InMemoryClientRegistrationRepository(getRegistration(provider));
	}

	private ClientRegistration getRegistration(@Nonnull String provider) {
		ClientRegistration.Builder builder = switch (provider.toLowerCase()) {
			case "google", "github", "facebook", "okta" -> {
				CommonOAuth2Provider commonProvider = EnumUtils.parse(CommonOAuth2Provider.class, provider);
				yield commonProvider.getBuilder(provider);
			}
			case "azure" -> createAzureBuilder();
			case "custom" -> createCustomBuilder(provider, provider.toLowerCase());
			default -> throw new IllegalStateException("unknown OAuth provider");
		};

		builder.clientId(clientId).clientSecret(clientSecret);

		builder.redirectUri(getRedirectUri());

		return builder.build();
	}

	private ClientRegistration.Builder createAzureBuilder() {
		if (StringUtils.isBlank(tenantId)) throw new IllegalStateException("when using Azure provider the tentantId property is required");

		ClientRegistration.Builder builder = ClientRegistration.withRegistrationId("azure");
		builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
		builder.authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE);

		// Use the default scopes but allow users to overwrite them
		builder.scope(StringUtil.split(StringUtils.isBlank(scopes) ? "openid,profile,email" : scopes));

		builder.authorizationUri("https://login.microsoftonline.com/%s/oauth2/v2.0/authorize".formatted(tenantId));
		builder.tokenUri("https://login.microsoftonline.com/%s/oauth2/v2.0/token".formatted(tenantId));
		builder.jwkSetUri("https://login.microsoftonline.com/common/discovery/v2.0/keys");
		builder.issuerUri("https://login.microsoftonline.com/%s/v2.0".formatted(tenantId));
		builder.userInfoUri("https://graph.microsoft.com/oidc/userinfo");
		builder.userNameAttributeName("email");
		builder.clientName("azure");
		return builder;
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


	/**
	 * Absolute base URL starts eg. `http(s)://{host}:{port}/` or is NULL (relative).
	 */
	@Nullable
	private String determineBaseUrl() {
		if (StringUtils.isEmpty(baseUrl)) {
			log.debug("using no baseUrl");
			return null;
		}

		final String computed;
		if(baseUrl.endsWith("/")) { // Ensure the url does not end with a slash
			computed = baseUrl.substring(0, baseUrl.length()-1);
		} else {
			computed = baseUrl;
		}

		log.debug("using baseUrl [{}]", computed);
		return computed;
	}

	/**
	 * When no base URL, spring uses {baseUrl} which resolves to: {baseScheme}://{baseHost}{basePort}{contextPath}.
	 * And when no base URL we must add the servlet-path our selves: "{baseUrl}" + servletPath;
	 * 
	 * When a base URL has been set, use that instead!
	 */
	@Nonnull
	private String computeRedirectUri() {
		String determinedBaseUrl = determineBaseUrl();

		if (determinedBaseUrl == null) {
			String path = servletPath.startsWith("/") ? servletPath.substring(1) : servletPath;
			return "{baseUrl}/%s/oauth2/code/{registrationId}".formatted(path);
		}

		return "%s/oauth2/code/{registrationId}".formatted(determinedBaseUrl);
	}

	/**
	 * Servlet-Path that needs to be secured. May not end with a `*` or `/`.
	 * For instance `/iaf/gui`.
	 */
	private String computeRelativePathFromServlet() {
		String servletPath = getPrivateEndpoints().stream().findFirst().orElse("");

		if(servletPath.endsWith("*")) { // Strip the '*' if the url ends with it
			servletPath = servletPath.substring(0, servletPath.length()-1);
		}

		if(servletPath.endsWith("/")) { // Ensure the url does not end with a slash
			servletPath = servletPath.substring(0, servletPath.length()-1);
		}

		log.debug("using oauth servlet-path [{}]", servletPath);
		return servletPath;
	}
}
