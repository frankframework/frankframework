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
package org.frankframework.http.authentication;

import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.Credentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;

import static org.frankframework.http.AbstractHttpSession.AUTHENTICATION_METHOD_KEY;

/**
 * HttpClient AuthScheme that uses OAuthAccessTokenManager to obtain an access token (via Client Credentials flow).
 *
 * @author Gerrit van Brakel
 *
 */
public class OAuthAuthenticationScheme extends BasicScheme {

	public static final String SCHEME_NAME_AUTO = "OAUTH2";
	public static final String SCHEME_NAME_FORCE_REFRESH = "OAUTH2-REFRESHED";

	private boolean forceRefresh;

	public OAuthAuthenticationScheme() {
		this(false);
	}

	public OAuthAuthenticationScheme(boolean forceRefresh) {
		super();
		this.forceRefresh = forceRefresh;
	}

	@Override
	public String getSchemeName() {
		return forceRefresh ? SCHEME_NAME_FORCE_REFRESH : SCHEME_NAME_AUTO;
	}

	@Override
	public Header authenticate(Credentials credentials, HttpRequest request, final HttpContext context) throws AuthenticationException {
		Args.notNull(credentials, "Credentials");
		Args.notNull(request, "HTTP request");

		IOauthAuthenticator oauthAuthentication = (IOauthAuthenticator) context.getAttribute(AUTHENTICATION_METHOD_KEY);

		if (oauthAuthentication == null) {
			throw new AuthenticationException("no oauthAuthentication found");
		}

		try {
			String accessToken = oauthAuthentication.getOrRefreshAccessToken(credentials, forceRefresh);

			return new BasicHeader(getHeaderName(), "Bearer " + accessToken);
		} catch (HttpAuthenticationException e) {
			throw new AuthenticationException(e.getMessage(), e);
		}
	}

	private String getHeaderName() {
		if (isProxy()) {
			return AUTH.PROXY_AUTH_RESP;
		}

		return AUTH.WWW_AUTH_RESP;
	}

	@Override
	public String toString() {
		return getSchemeName() + " [complete=" + isComplete() + "]";
	}

}
