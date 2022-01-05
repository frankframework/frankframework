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

import java.util.Map;
import java.util.Queue;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.protocol.HttpContext;

/**
 * OAuth prefering AuthenticationStrategy.
 * 
 * @author Gerrit van Brakel
 *
 */
public class OAuthPreferringAuthenticationStrategy extends SingleSchemePreferringAuthenticationStrategy {
	
	private OAuthAccessTokenManager accessTokenManager;
	
	public OAuthPreferringAuthenticationStrategy(OAuthAccessTokenManager accessTokenManager) {
		super(new OAuthAuthenticationScheme());
		this.accessTokenManager = accessTokenManager;
	}
	
	@Override
	public Queue<AuthOption> select(Map<String, Header> challenges, HttpHost authhost, HttpResponse response, HttpContext context) throws MalformedChallengeException {
		context.setAttribute(OAuthAuthenticationScheme.ACCESSTOKEN_MANAGER_KEY, accessTokenManager);
		return super.select(challenges, authhost, response, context);
	}

}
