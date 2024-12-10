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

import static org.frankframework.util.StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import lombok.extern.log4j.Log4j2;

import org.frankframework.http.AbstractHttpSession;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.JacksonUtils;

@Log4j2
public abstract class AbstractOauthAuthenticator implements IOauthAuthenticator {

	protected final AbstractHttpSession session;
	protected final URI authorizationEndpoint;
	protected final int overwriteExpiryMs;

	private String accessToken;
	private long accessTokenRefreshTime;

	AbstractOauthAuthenticator(final AbstractHttpSession session) throws HttpAuthenticationException {
		this.session = session;
		try {
			this.authorizationEndpoint = new URI(session.getTokenEndpoint());
		} catch (URISyntaxException e) {
			throw new HttpAuthenticationException(e);
		}
		this.overwriteExpiryMs = session.getTokenExpiry() * 1000;
	}

	protected abstract HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException;

	protected HttpEntityEnclosingRequestBase createPostRequestWithForm(URI uri, List<NameValuePair> formParameters) throws HttpAuthenticationException {
		try {
			UrlEncodedFormEntity body = new UrlEncodedFormEntity(formParameters, DEFAULT_INPUT_STREAM_ENCODING);

			HttpPost request = new HttpPost(uri);
			request.addHeader(body.getContentType());
			request.setEntity(body);

			return request;
		} catch (UnsupportedEncodingException e) {
			throw new HttpAuthenticationException(e);
		}
	}

	private void refreshAccessToken(Credentials credentials) throws HttpAuthenticationException {
		System.err.println("Refreshing access token, called from: ");
		new Exception("Method called from:").printStackTrace(System.err);
		HttpRequestBase request = createRequest(credentials, new ArrayList<>());

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

			accessToken = dto.getAccessToken();
			long accessTokenLifetime = Long.parseLong(dto.getExpiresIn());

			// accessToken will be refreshed when it is half way expiration
			if (overwriteExpiryMs < 0 && accessTokenLifetime == 0) {
				log.debug("no accessToken lifetime found in accessTokenResponse, and no expiry specified. Token will not be refreshed preemptively");
				accessTokenRefreshTime = -1;
			} else {
				accessTokenRefreshTime = System.currentTimeMillis() + (overwriteExpiryMs < 0 ? 500 * accessTokenLifetime : overwriteExpiryMs);
				log.debug("set accessTokenRefreshTime [{}]", ()-> DateFormatUtils.format(accessTokenRefreshTime));
			}
		} catch (IOException e) {
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

	public final String getOrRefreshAccessToken(Credentials credentials, boolean forceRefresh) throws HttpAuthenticationException {
		if (forceRefresh || accessToken == null || accessTokenRefreshTime > 0 && System.currentTimeMillis() > accessTokenRefreshTime) {
			log.debug("Refreshing accessToken");
			refreshAccessToken(credentials);
		}

		return accessToken;
	}
}
