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

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AUTH;
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

/**
 * OAuth prefering AuthenticationStrategy.
 * 
 * @author Gerrit van Brakel
 *
 */
public class OAuthPreferringAuthenticationStrategy extends TargetAuthenticationStrategy {
	protected Logger log = LogUtil.getLogger(this);
	
	private boolean refreshTokenOn401; // retrying unchallenged request/responses might cause endless authentication loops

	@Override
	public Queue<AuthOption> select(Map<String, Header> challenges, HttpHost authhost, HttpResponse response, HttpContext context) throws MalformedChallengeException {
		final HttpClientContext clientContext = HttpClientContext.adapt(context);

		final Queue<AuthOption> options = new LinkedList<AuthOption>();

		final CredentialsProvider credsProvider = clientContext.getCredentialsProvider();
		if (credsProvider == null) {
			this.log.debug("Credentials provider not set in the context");
			return options;
		}

		final AuthScope authScope = new AuthScope(authhost, "", OAuthAuthenticationScheme.SCHEME_NAME);
		final Credentials credentials = credsProvider.getCredentials(authScope);
		if (credentials != null) {
			options.add(new AuthOption(new OAuthAuthenticationScheme(), credentials));
		}

		options.addAll(super.select(challenges, authhost, response, clientContext));
		return options;
	}

	@Override
	public Map<String, Header> getChallenges(HttpHost authhost, HttpResponse response, HttpContext context) throws MalformedChallengeException {
		Map<String, Header> result = super.getChallenges(authhost, response, context);
		if (refreshTokenOn401 && !result.containsKey(AUTH.WWW_AUTH)) {
			// retrying unchallenged request/responses might cause endless authentication loops
			result.put(AUTH.WWW_AUTH, new BasicHeader(AUTH.WWW_AUTH,"oauth"));
		}
		return result;
	}

}
