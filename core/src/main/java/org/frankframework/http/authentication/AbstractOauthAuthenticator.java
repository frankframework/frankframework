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
package org.frankframework.http.authentication;

import org.apache.http.auth.Credentials;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.http.AbstractHttpSession;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.JacksonUtils;
import org.frankframework.util.LogUtil;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public abstract class AbstractOauthAuthenticator implements IAuthenticator {

	protected final Logger log = LogUtil.getLogger(this);

	protected final AbstractHttpSession session;
	protected final URI authorizationEndpoint;
	protected final int expiryMs;

	private String accessToken;
	private long accessTokenRefreshTime;

	public AbstractOauthAuthenticator(final AbstractHttpSession session) throws HttpAuthenticationException {
		this.session = session;
		try {
			this.authorizationEndpoint = new URI(session.getTokenEndpoint());
		} catch (URISyntaxException e) {
			throw new HttpAuthenticationException(e);
		}
		this.expiryMs = session.getTokenExpiry() * 1000;
	}

	abstract public boolean validate(AbstractHttpSession session);
	abstract protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials) throws HttpAuthenticationException;

	private void refreshAccessToken(Credentials credentials) throws HttpAuthenticationException {
		HttpRequestBase request = createRequest(credentials);

		CloseableHttpClient apacheHttpClient = session.getHttpClient();
		TimeoutGuard tg = new TimeoutGuard(1 + session.getTimeout() / 1000, "token retrieval") {

			@Override
			protected void abort() {
				request.abort();
			}

		};

		try (CloseableHttpResponse response = apacheHttpClient.execute(request)) {
			String responseBody = EntityUtils.toString(response.getEntity());

			if (response.getStatusLine().getStatusCode() != 200) {
				throw new HttpAuthenticationException(responseBody);
			}

			OauthResponseDto dto = JacksonUtils.convertToDTO(responseBody, OauthResponseDto.class);

			accessToken = "Bearer " + dto.getAccess_token();
			long accessTokenLifetime = Long.parseLong(dto.getExpires_in());

			// accessToken will be refreshed when it is half way expiration
			if (expiryMs < 0 && accessTokenLifetime == 0) {
				log.debug("no accessToken lifetime found in accessTokenResponse, and no expiry specified. Token will not be refreshed preemptively");
				accessTokenRefreshTime = -1;
			} else {
				accessTokenRefreshTime = System.currentTimeMillis() + (expiryMs<0 ? 500 * accessTokenLifetime : expiryMs);
				log.debug("set accessTokenRefreshTime [{}]", ()-> DateFormatUtils.format(accessTokenRefreshTime));
			}
		} catch (IOException e) {
			request.abort();
			throw new HttpAuthenticationException(e);
		} finally {
			if (tg.cancel()) {
				throw new HttpAuthenticationException("timeout of [" + session.getTimeout() + "] ms exceeded");
			}
		}
	}

	public final String getOrRefreshAccessToken(Credentials credentials, boolean forceRefresh) throws HttpAuthenticationException {
		if (forceRefresh || accessToken == null || accessTokenRefreshTime > 0 && System.currentTimeMillis() > accessTokenRefreshTime) {
			log.debug("Refreshing accessToken");
			refreshAccessToken(credentials);
		}

		return accessToken;
	}
}
