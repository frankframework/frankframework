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
package nl.nn.adapterframework.http.authentication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
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
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

import nl.nn.adapterframework.http.HttpSessionBase;
import nl.nn.adapterframework.task.TimeoutGuard;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.StringUtil;

public class OAuthAccessTokenManager {
	protected Logger log = LogUtil.getLogger(this);

	private URI tokenEndpoint;
	private final Scope scope;
	private final CredentialFactory clientCredentialFactory;
	private final boolean useClientCredentialsGrant;
	private final HttpSessionBase httpSession;
	private final int expiryMs;
	private final boolean authenticatedTokenRequest; // if set true, clientId and clientSecret will be added as Basic Authentication header, instead of as request parameters

	private AccessToken accessToken;
	private long accessTokenRefreshTime;

	public OAuthAccessTokenManager(String tokenEndpoint, String scope, CredentialFactory clientCF, boolean useClientCredentialsGrant, boolean authenticatedTokenRequest, HttpSessionBase httpSession, int expiry) throws HttpAuthenticationException {
		try {
			this.tokenEndpoint = new URI(tokenEndpoint);
		} catch (URISyntaxException e) {
			throw new HttpAuthenticationException("illegal token endpoint", e);
		}

		this.scope = StringUtils.isNotEmpty(scope) ? Scope.parse(scope) : null;
		this.clientCredentialFactory = clientCF;
		this.useClientCredentialsGrant = useClientCredentialsGrant;
		this.authenticatedTokenRequest = authenticatedTokenRequest;
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

		if (useClientCredentialsGrant) {
			grant = new ClientCredentialsGrant();
		} else {
			String username = credentials.getUserPrincipal().getName();
			Secret password = new Secret(credentials.getPassword());
			grant = new ResourceOwnerPasswordCredentialsGrant(username, password);
		}

		// The credentials to authenticate the client at the token endpoint
		ClientID clientID = new ClientID(clientCredentialFactory.getUsername());
		Secret clientSecret = new Secret(clientCredentialFactory.getPassword());
		ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

		return new TokenRequest(tokenEndpoint, clientAuth, grant, scope);
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
				log.debug("set accessTokenRefreshTime [{}]", ()->DateUtils.format(accessTokenRefreshTime));
			}
		} catch (ParseException e) {
			throw new HttpAuthenticationException("Could not parse TokenResponse: "+httpResponse.getContent(), e);
		}
	}

	// convert the Nimbus HTTPRequest into an Apache HttpClient HttpRequest
	protected HttpRequestBase convertToApacheHttpRequest(HTTPRequest httpRequest) throws HttpAuthenticationException {
		HttpRequestBase apacheHttpRequest;
		String query = httpRequest.getQuery();
		if (!authenticatedTokenRequest) {
			List<NameValuePair> clientInfo= new LinkedList<>();
			clientInfo.add(new BasicNameValuePair("client_id", clientCredentialFactory.getUsername()));
			clientInfo.add(new BasicNameValuePair("client_secret", clientCredentialFactory.getPassword()));
			query = StringUtil.concatStrings(query, "&", URLEncodedUtils.format(clientInfo, "UTF-8"));
		}
		switch (httpRequest.getMethod()) {
			case GET:
				String url = StringUtil.concatStrings(httpRequest.getURL().toExternalForm(), "?", query);
				apacheHttpRequest = new HttpGet(url);
				break;
			case POST:
				apacheHttpRequest = new HttpPost(httpRequest.getURL().toExternalForm());
				apacheHttpRequest.addHeader("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
				try {
					((HttpPost)apacheHttpRequest).setEntity(new StringEntity(query));
				} catch (UnsupportedEncodingException e) {
					throw new HttpAuthenticationException("Could not create TokenRequest", e);
				}

				break;
			default:
				throw new IllegalStateException("Illegal Method, must be GET or POST");
		}
		httpRequest.getHeaderMap().forEach((k,l) -> l.forEach(v -> apacheHttpRequest.addHeader(k, v)));
		return apacheHttpRequest;
	}

	private HTTPResponse convertFromApacheHttpResponse(CloseableHttpResponse apacheHttpResponse) throws HttpAuthenticationException, UnsupportedOperationException, IOException {
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
