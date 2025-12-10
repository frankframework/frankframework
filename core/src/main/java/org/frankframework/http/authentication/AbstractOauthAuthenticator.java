/*
   Copyright 2024 - 2025 WeAreFrank!

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
package org.frankframework.http.authentication;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.http.AbstractHttpSession;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.TimeProvider;

@Log4j2
public abstract class AbstractOauthAuthenticator implements IOauthAuthenticator {

	protected final AbstractHttpSession session;

	protected final URI authorizationEndpoint;
	protected final int overwriteExpiryMs;

	protected final String username;
	protected final String password;
	protected final String clientId;
	protected final String clientSecret;

	private AccessToken accessToken;
	private long accessTokenRefreshTime;

	AbstractOauthAuthenticator(final AbstractHttpSession session) throws HttpAuthenticationException {
		this.session = session;

		if (StringUtils.isBlank(session.getTokenEndpoint())) {
			throw new HttpAuthenticationException("no tokenEndpoint provided");
		}

		try {
			this.authorizationEndpoint = new URI(session.getTokenEndpoint());
		} catch (URISyntaxException e) {
			throw new HttpAuthenticationException(e);
		}
		this.overwriteExpiryMs = session.getTokenExpiry() * 1000;

		CredentialFactory userCredentials = session.getCredentials();
		this.username = userCredentials.getUsername();
		this.password = userCredentials.getPassword();

		CredentialFactory clientCredentials = new CredentialFactory(session.getClientAuthAlias(), session.getClientId(), session.getClientSecret());
		this.clientId = clientCredentials.getUsername();
		this.clientSecret = clientCredentials.getPassword();
	}

	@Nullable
	protected BasicHeader getScopeHeader() {
		if (session.getScope() != null) {
			return new BasicHeader("scope", session.getScope().replace(',', ' '));
		}

		return null;
	}

	protected HttpEntityEnclosingRequestBase createPostRequestWithForm(URI uri, List<NameValuePair> formParameters) throws HttpAuthenticationException {
		UrlEncodedFormEntity body = new UrlEncodedFormEntity(formParameters, StreamUtil.DEFAULT_CHARSET);

		HttpPost request = new HttpPost(uri);
		request.addHeader(body.getContentType());
		request.setEntity(body);

		return request;
	}

	protected abstract HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException;

	private void refreshAccessToken(Credentials credentials) throws HttpAuthenticationException {
		log.debug("Refreshing access token");
		HttpRequestBase request = createRequest(credentials, new ArrayList<>());

		CloseableHttpClient apacheHttpClient = session.getHttpClient();
		TimeoutGuard tg = new TimeoutGuard(1 + session.getTimeout() / 1000, "token retrieval", request::abort);

		try (CloseableHttpResponse response = apacheHttpClient.execute(request)) {
			AccessTokenResponse successResponse = getTokenResponse(response);

			accessToken = successResponse.getTokens().getAccessToken();
			long accessTokenLifetime = accessToken.getLifetime();
			// accessToken will be refreshed when it is half way expiration
			if (overwriteExpiryMs < 0 && accessTokenLifetime == 0) {
				log.debug("no accessToken lifetime found in accessTokenResponse, and no expiry specified. Token will not be refreshed preemptively");
				accessTokenRefreshTime = -1;
			} else {
				accessTokenRefreshTime = TimeProvider.nowAsMillis() + (overwriteExpiryMs < 0 ? 500 * accessTokenLifetime : overwriteExpiryMs);
				log.debug("set accessTokenRefreshTime [{}]", ()-> DateFormatUtils.format(accessTokenRefreshTime));
			}
		} catch (HttpAuthenticationException e) {
			log.debug("Failed to refresh access token, got an HttpAuthenticationException: {}", e.getMessage());
			tg.cancel();
			throw e;
		} catch (IOException e) {
			log.debug("Failed to refresh access token, got an exception: {}", e.getMessage());
			request.abort();

			if (tg.cancel()) {
				throw new HttpAuthenticationException("timeout of [" + session.getTimeout() + "] ms exceeded", e);
			}

			throw new HttpAuthenticationException(e);
		}

		if (tg.cancel()) {
			throw new HttpAuthenticationException("timeout of [" + session.getTimeout() + "] ms exceeded");
		}
	}

	private AccessTokenResponse getTokenResponse(CloseableHttpResponse response) throws HttpAuthenticationException {
		try {
			String responseBody = EntityUtils.toString(response.getEntity());

			if (response.getStatusLine().getStatusCode() != HttpServletResponse.SC_OK) {
				log.debug("Failed to refresh access token, received status code {}", response.getStatusLine().getStatusCode());
				throw new HttpAuthenticationException(responseBody);
			}

			return AccessTokenResponse.parse(JSONObjectUtils.parse(responseBody));
		} catch (IOException | org.apache.http.ParseException | com.nimbusds.oauth2.sdk.ParseException e) {
			throw new HttpAuthenticationException("unable to parse access token response", e);
		}
	}

	@Override
	public final String getOrRefreshAccessToken(Credentials credentials, boolean forceRefresh) throws HttpAuthenticationException {
		if (forceRefresh || accessToken == null || accessTokenRefreshTime > 0 && System.currentTimeMillis() > accessTokenRefreshTime) {
			log.debug("getOrRefreshAccessToken");
			refreshAccessToken(credentials);
		} else {
			log.debug("reusing cached accessToken");
		}

		return accessToken.toAuthorizationHeader();
	}
}
