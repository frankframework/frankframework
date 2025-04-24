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

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.message.BasicNameValuePair;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.http.AbstractHttpSession;
import org.frankframework.util.CredentialFactory;

@Log4j2
public abstract class AbstractClientCredentials extends AbstractOauthAuthenticator {
	private final String clientId;
	private final String clientSecret;

	AbstractClientCredentials(AbstractHttpSession session) throws HttpAuthenticationException {
		super(session);

		CredentialFactory credentials = session.getCredentials();
		this.clientId = credentials.getUsername();
		this.clientSecret = credentials.getPassword();
	}

	@Override
	public final void configure() throws ConfigurationException {
		if (clientId == null) {
			throw new ConfigurationException("clientId is required");
		}

		if (clientSecret == null) {
			throw new ConfigurationException("clientSecret is required");
		}

		if (session.getUsername() != null) {
			ConfigurationWarnings.add(session, log, "Username should not be set");
		}

		if (session.getPassword() != null) {
			ConfigurationWarnings.add(session, log, "Password should not be set");
		}

		if (session.getAuthAlias() != null) {
			ConfigurationWarnings.add(session, log, "AuthAlias should not be set");
		}
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException {
		parameters.add(new BasicNameValuePair("grant_type", "client_credentials"));

		if (session.getScope() != null) {
			parameters.add(new BasicNameValuePair("scope", session.getScope().replace(',', ' ')));
		}

		return createPostRequestWithForm(authorizationEndpoint, parameters);
	}

}
