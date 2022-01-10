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

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BufferedHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.CharArrayBuffer;

/**
 * HttpClient AuthScheme that uses ClientCredentials flow.
 * N.B. The scheme is stateful, it can manage only a single token.
 * 
 * @author Gerrit van Brakel
 *
 */
public class OAuthAuthenticationScheme extends BasicScheme {

	public final static String SCHEME_NAME = "OAUTH2";
	public final static String ACCESSTOKEN_MANAGER_KEY="AccessTokenManager";
	
	@Override
	public String getSchemeName() {
		return SCHEME_NAME;
	}

	@Override
	public Header authenticate(Credentials credentials, HttpRequest request, final HttpContext context) throws AuthenticationException {
		Args.notNull(credentials, "Credentials");
		Args.notNull(request, "HTTP request");
		
		OAuthAccessTokenManager accessTokenManager = (OAuthAccessTokenManager)context.getAttribute(ACCESSTOKEN_MANAGER_KEY);
		if (accessTokenManager==null) {
			throw new AuthenticationException("no accessTokenManager found");
		}

		try {
			String accessToken = accessTokenManager.getAccessToken(credentials);
			final CharArrayBuffer buffer = new CharArrayBuffer(32);
			if (isProxy()) {
				buffer.append(AUTH.PROXY_AUTH_RESP);
			} else {
				buffer.append(AUTH.WWW_AUTH_RESP);
			}
			buffer.append(": ");
			buffer.append(accessToken);

			return new BufferedHeader(buffer);
		} catch (HttpAuthenticationException e) {
			throw new AuthenticationException(e.getMessage(), e);
		}
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getSchemeName()+" [complete=").append(isComplete()).append("]");
		return builder.toString();
	}

}
