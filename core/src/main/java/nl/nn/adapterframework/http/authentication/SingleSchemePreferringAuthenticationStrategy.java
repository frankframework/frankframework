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
import org.apache.http.auth.AuthOption;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.MalformedChallengeException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.TargetAuthenticationStrategy;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

/**
 * AuthenticationStrategy that prefers a specific (non standard) scheme.
 * 
 * @author Gerrit van Brakel
 *
 */
public class SingleSchemePreferringAuthenticationStrategy extends TargetAuthenticationStrategy {
	protected Logger log = LogUtil.getLogger(this);

	private AuthScheme authScheme;

	public SingleSchemePreferringAuthenticationStrategy(AuthScheme authScheme) {
		super();
		this.authScheme = authScheme;
	}
	
	@Override
	public Queue<AuthOption> select(Map<String, Header> challenges, HttpHost authhost, HttpResponse response, HttpContext context) throws MalformedChallengeException {
		final HttpClientContext clientContext = HttpClientContext.adapt(context);

		final Queue<AuthOption> options = new LinkedList<AuthOption>();

		final CredentialsProvider credsProvider = clientContext.getCredentialsProvider();
		if (credsProvider == null) {
			this.log.debug("Credentials provider not set in the context");
			return options;
		}

		final AuthScope authScope = new AuthScope(authhost, authScheme.getRealm(), authScheme.getSchemeName());
		final Credentials credentials = credsProvider.getCredentials(authScope);
		if (credentials != null) {
			options.add(new AuthOption(authScheme, credentials));
		}

		options.addAll(super.select(challenges, authhost, response, clientContext));
		return options;
	}

}
