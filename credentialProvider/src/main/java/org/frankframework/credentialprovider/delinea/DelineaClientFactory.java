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

import static org.springframework.web.util.UriComponentsBuilder.fromUriString;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilderFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Used to create a {@link DelineaClient} based on  the given {@link DelineaClientSettings}.
 *
 * @author evandongen
 */
public class DelineaClientFactory {
	private static final String GRANT_REQUEST_USERNAME_PROPERTY = "username";

	private static final String GRANT_REQUEST_PASSWORD_PROPERTY = "password";

	private static final String GRANT_REQUEST_GRANT_TYPE_PROPERTY = "grant_type";

	private static final String GRANT_REQUEST_GRANT_TYPE = "password";

	private static final String AUTHORIZATION_HEADER_NAME = "Authorization";

	private static final String AUTHORIZATION_TOKEN_TYPE = "Bearer";

	private final ClientHttpRequestFactory requestFactory;

	private final UriBuilderFactory uriBuilderFactory;

	private final DelineaClientSettings delineaClientSettings;

	public DelineaClientFactory(DelineaClientSettings delineaClientSettings) {
		uriBuilderFactory = new DefaultUriBuilderFactory(fromUriString(
				formatUrlTemplateOrGetUrl(delineaClientSettings.apiRootUrlTemplate(), delineaClientSettings.apiRootUrl())));

		this.delineaClientSettings = delineaClientSettings;

		requestFactory = new SimpleClientHttpRequestFactory();
	}

	public DelineaClient getObject() {
		final DelineaClient delineaClient = new DelineaClient();

		delineaClient.setUriTemplateHandler(uriBuilderFactory);
		delineaClient.setRequestFactory( // Add the 'Authorization: Bearer {accessGrant.accessToken}' HTTP header
				new InterceptingClientHttpRequestFactory(requestFactory, List.of((request, body, execution) -> {
					request.getHeaders().add(
							AUTHORIZATION_HEADER_NAME,
							String.format("%s %s", AUTHORIZATION_TOKEN_TYPE, getAccessGrant().accessToken)
					);
					return execution.execute(request, body);
				})));

		return delineaClient;
	}

	private AccessGrant getAccessGrant() {
		final MultiValueMap<String, String> request = new LinkedMultiValueMap<>();

		request.add(GRANT_REQUEST_USERNAME_PROPERTY, delineaClientSettings.oauthUsername());
		request.add(GRANT_REQUEST_PASSWORD_PROPERTY, delineaClientSettings.oauthPassword());
		request.add(GRANT_REQUEST_GRANT_TYPE_PROPERTY, GRANT_REQUEST_GRANT_TYPE);

		return new RestTemplate().postForObject(
				formatUrlTemplateOrGetUrl(delineaClientSettings.tokenUrlTemplate(), delineaClientSettings.oauthTokenUrl()),
				request, AccessGrant.class);
	}

	private String formatUrlTemplateOrGetUrl(String urlTemplate, String url) {
		return StringUtils.isNotEmpty(delineaClientSettings.tenant()) ? String.format(urlTemplate.replaceAll("/*$", ""),
				delineaClientSettings.tenant(), delineaClientSettings.tld())
				: url.replaceAll("/*$", "");
	}

	record AccessGrant(
		@JsonProperty("access_token")
		String accessToken,

		@JsonProperty("refresh_token")
		String refreshToken,

		@JsonProperty("token_type")
		String tokenType,

		@JsonProperty("expires_in")
		int expiresIn) {
	}
}
