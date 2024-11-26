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

import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import org.apache.http.message.BasicNameValuePair;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.http.AbstractHttpSession;

import java.util.ArrayList;
import java.util.List;

public class ResourceOwnerPasswordCredentialsQueryParameters extends AbstractOauthAuthenticator {

	public ResourceOwnerPasswordCredentialsQueryParameters(AbstractHttpSession session) throws HttpAuthenticationException {
		super(session);
	}

	@Override
	public void configure() throws ConfigurationException {
		if (session.getClientId() == null) {
			throw new ConfigurationException("clientId is required");
		}

		if (session.getClientSecret() == null) {
			throw new ConfigurationException("clientSecret is required");
		}
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials) throws HttpAuthenticationException {
		List<NameValuePair> parameters = new ArrayList<>();
		parameters.add(new BasicNameValuePair("grant_type", "password"));

		if (session.getScope() != null) {
			parameters.add(new BasicNameValuePair("scope", session.getScope().replace(',', ' ')));
		}

		parameters.add(new BasicNameValuePair("username", credentials.getUserPrincipal().getName()));
		parameters.add(new BasicNameValuePair("password", credentials.getPassword()));

		parameters.add(new BasicNameValuePair("client_id", session.getClientId()));
		parameters.add(new BasicNameValuePair("client_secret", session.getClientSecret()));

		return createPostRequestWithForm(authorizationEndpoint, parameters);
	}
}
