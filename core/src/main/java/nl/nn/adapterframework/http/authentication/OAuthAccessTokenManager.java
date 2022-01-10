/*
   Copyright 2022 WeAreFrank!

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
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.auth.Credentials;

import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.TokenResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;

public class OAuthAccessTokenManager {

	private String tokenEndpoint;
	private String scope[];
	
	private AccessToken accessToken;
	private long accessTokenIssuedAt;
	
	public OAuthAccessTokenManager(String tokenEndpoint, String...scope) {
		this.tokenEndpoint = tokenEndpoint;
		this.scope = scope;
	}
		
	public synchronized void retrieveAccessToken(Credentials credentials) throws HttpAuthenticationException {
		// Construct the client credentials grant
		AuthorizationGrant clientGrant = new ClientCredentialsGrant();

		// The credentials to authenticate the client at the token endpoint
		ClientID clientID = new ClientID(credentials.getUserPrincipal().getName());
		Secret clientSecret = new Secret(credentials.getPassword());
		ClientAuthentication clientAuth = new ClientSecretBasic(clientID, clientSecret);

		// The request scope for the token (may be optional)
		Scope _scope = scope.length>1 && scope[0]!=null ? new Scope(scope) : null;

		try {
			// The token endpoint
			URI _tokenEndpoint = new URI(tokenEndpoint);
			// Make the token request
			TokenRequest request = new TokenRequest(_tokenEndpoint, clientAuth, clientGrant, _scope);

			try {
				HTTPResponse httpResponse = request.toHTTPRequest().send();
				if (httpResponse.getStatusCode()!=200) {
					throw new HttpAuthenticationException("Could not retrieve token: ("+httpResponse.getStatusCode()+") "+httpResponse.getStatusMessage());
				}
				
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
					accessTokenIssuedAt = System.currentTimeMillis();
				} catch (ParseException e) {
					throw new HttpAuthenticationException("Could not parse TokenResponse: "+httpResponse.getContent(), e);
				}
			} catch (IOException e) {
				throw new HttpAuthenticationException("Could not send TokenRequest", e);
			}
		} catch (URISyntaxException e) {
			throw new HttpAuthenticationException("illegal token endpoint", e);
		}
	}
	
	public String getAccessToken(Credentials credentials) throws HttpAuthenticationException {
		if (accessToken==null || System.currentTimeMillis() > accessTokenIssuedAt + 500 * accessToken.getLifetime()) {
			// retrieve a fresh token if there is none, or it is half way expiration
			retrieveAccessToken(credentials);
		}
		return accessToken.toAuthorizationHeader();
	}
}
