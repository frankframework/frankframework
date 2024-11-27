/*
   Copyright 2022-2023 WeAreFrank!

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
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import org.frankframework.http.AbstractHttpSession;
import org.frankframework.task.TimeoutGuard;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;

public class OAuthAccessTokenManager {
	protected Logger log = LogUtil.getLogger(this);

	private final URI tokenEndpoint;
	private final Scope scope;
	private final CredentialFactory clientCredentialFactory;
	private final boolean useClientCredentials;
	private final AbstractHttpSession httpSession;
	private final int expiryMs;
	private final AuthenticationType authenticationType;

	private AccessToken accessToken;
	private long accessTokenRefreshTime;

	public enum AuthenticationType {
		AUTHENTICATION_HEADER, REQUEST_PARAMETER
	}

	public OAuthAccessTokenManager(String tokenEndpoint, String scope, CredentialFactory clientCF, boolean useClientCredentials, AuthenticationType authType, AbstractHttpSession httpSession, int expiry) throws HttpAuthenticationException {
		try {
			this.tokenEndpoint = new URI(tokenEndpoint);
		} catch (URISyntaxException e) {
			throw new HttpAuthenticationException("illegal token endpoint", e);
		}

		this.scope = StringUtils.isNotEmpty(scope) ? Scope.parse(scope) : null;
		this.clientCredentialFactory = clientCF;
		this.useClientCredentials = useClientCredentials;
		this.authenticationType = authType;
		this.httpSession = httpSession;
		this.expiryMs = expiry * 1000;
	}


	public synchronized void retrieveAccessToken(Credentials credentials) throws HttpAuthenticationException {
		TokenRequest request = createRequest(credentials);
		HttpRequestBase apacheHttpRequest = convertToApacheHttpRequest(request.toHTTPRequest());

		CloseableHttpClient apacheHttpClient = httpSession.getHttpClient();
		TimeoutGuard tg = new TimeoutGuard(1+httpSession.getTimeout()/1000, "token retrieval") {

			@Override
			protected void abort() {
				apacheHttpRequest.abort();
			}

		};
		try (CloseableHttpResponse apacheHttpResponse = apacheHttpClient.execute(apacheHttpRequest)) {

			HTTPResponse httpResponse = convertFromApacheHttpResponse(apacheHttpResponse);
			parseResponse(httpResponse);
		} catch (IOException e) {
			apacheHttpRequest.abort();
			throw new HttpAuthenticationException(e);
		} finally {
			if (tg.cancel()) {
				throw new HttpAuthenticationException("timeout of ["+httpSession.getTimeout()+"] ms exceeded");
			}
		}
	}

	protected TokenRequest createRequest(Credentials credentials) {
		AuthorizationGrant grant;

		if (useClientCredentials) { // Client authentication is required
			grant = new ClientCredentialsGrant();
		} else { // Client authentication required only for confidential clients
			String username = credentials.getUserPrincipal().getName();
			Secret password = new Secret(credentials.getPassword());
			grant = new ResourceOwnerPasswordCredentialsGrant(username, password);
		}

		// The credentials to authenticate the client at the token endpoint
		ClientID clientID = new ClientID(clientCredentialFactory.getUsername());
		Secret clientSecret = new Secret(clientCredentialFactory.getPassword());
		ClientAuthentication clientAuthentication = getClientAuthentication(clientID, clientSecret);

		return new TokenRequest(tokenEndpoint, clientAuthentication, grant, scope);
	}

	private ClientAuthentication getClientAuthentication(ClientID clientID, Secret clientSecret) {
		if (authenticationType == AuthenticationType.REQUEST_PARAMETER) {
			// When using request parameter, we need to use ClientSecretPost which will convert the secret to a queryString
			return new ClientSecretPost(clientID, clientSecret);
		} else if (authenticationType == AuthenticationType.AUTHENTICATION_HEADER) {
			// When using authentication header, we need to use ClientSecretBasic because that will set the needed authentication header
			return new ClientSecretBasic(clientID, clientSecret);
		}

		throw new IllegalStateException("Illegal authentication type: " + authenticationType);
	}

	private void parseResponse(HTTPResponse httpResponse) throws HttpAuthenticationException {
		try {
			TokenResponse response = TokenResponse.parse(httpResponse);
			if (! response.indicatesSuccess()) {
				// We got an error response...
				TokenErrorResponse errorResponse = response.toErrorResponse();
				throw new HttpAuthenticationException(errorResponse.toJSONObject().toString());
			}

			AccessTokenResponse successResponse = response.toSuccessResponse();

			// Get the access token
			accessToken = successResponse.getTokens().getAccessToken();
			// accessToken will be refreshed when it is half way expiration
			long accessTokenLifetime = accessToken.getLifetime();
			if (expiryMs<0 && accessTokenLifetime==0) {
				log.debug("no accessToken lifetime found in accessTokenResponse, and no expiry specified. Token will not be refreshed preemptively");
				accessTokenRefreshTime = -1;
			} else {
				accessTokenRefreshTime = System.currentTimeMillis() + (expiryMs<0 ? 500 * accessTokenLifetime : expiryMs);
				log.debug("set accessTokenRefreshTime [{}]", ()-> DateFormatUtils.format(accessTokenRefreshTime));
			}
		} catch (ParseException e) {
			throw new HttpAuthenticationException("Could not parse TokenResponse: "+httpResponse.getContent(), e);
		}
	}

	// convert the Nimbus HTTPRequest into an Apache HttpClient HttpRequest
	protected HttpRequestBase convertToApacheHttpRequest(HTTPRequest httpRequest) throws HttpAuthenticationException {
		HttpRequestBase apacheHttpRequest;
		String query = httpRequest.getQuery(); //This is the POST BODY, don't ask me why they called it QUERY...

		switch (httpRequest.getMethod()) {
			case GET:
				String url = StringUtil.concatStrings(httpRequest.getURL().toExternalForm(), "?", query);
				apacheHttpRequest = new HttpGet(url);
				break;
			case POST: //authenticationType.HEADER is always POST
				apacheHttpRequest = new HttpPost(httpRequest.getURL().toExternalForm());
				ContentType contentType = ContentType.APPLICATION_FORM_URLENCODED.withCharset(StandardCharsets.UTF_8);
				((HttpPost)apacheHttpRequest).setEntity(new StringEntity(query, contentType));

				break;
			default:
				throw new IllegalStateException("Illegal Method, must be GET or POST");
		}

		httpRequest.getHeaderMap().forEach((k,l) -> l.forEach(v -> apacheHttpRequest.addHeader(k, v)));
		return apacheHttpRequest;
	}

	protected HTTPResponse convertFromApacheHttpResponse(CloseableHttpResponse apacheHttpResponse) throws HttpAuthenticationException, UnsupportedOperationException, IOException {
		StatusLine statusLine = apacheHttpResponse.getStatusLine();

		String responseBody = null;
		HttpEntity entity = apacheHttpResponse.getEntity();
		if(entity != null) {
			responseBody = StreamUtil.streamToString(entity.getContent(), null, null);
			EntityUtils.consume(entity);
		}

		if (statusLine.getStatusCode()!=200) {
			throw new HttpAuthenticationException("Could not retrieve token: ("+statusLine.getStatusCode()+") "+statusLine.getReasonPhrase()+": "+responseBody);
		}

		HTTPResponse httpResponse = new HTTPResponse(statusLine.getStatusCode());
		httpResponse.setStatusMessage(statusLine.getReasonPhrase());
		for(Header header:apacheHttpResponse.getAllHeaders()) {
			httpResponse.setHeader(header.getName(), header.getValue());
		}

		if(responseBody != null) {
			httpResponse.setContent(responseBody);
		}
		return httpResponse;
	}

	public String getAccessToken(Credentials credentials, boolean forceRefresh) throws HttpAuthenticationException {
		if (forceRefresh || accessToken==null || accessTokenRefreshTime>0 && System.currentTimeMillis() > accessTokenRefreshTime) {
			log.debug("refresh accessToken");
			retrieveAccessToken(credentials);
		} else {
			log.debug("reusing cached accessToken");
		}
		return accessToken.toAuthorizationHeader();
	}
}
