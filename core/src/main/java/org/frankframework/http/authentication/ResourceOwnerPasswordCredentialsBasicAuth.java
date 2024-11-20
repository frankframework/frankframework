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

import com.nimbusds.jose.util.Base64;

import org.apache.http.HttpHeaders;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import org.apache.http.message.BasicHeader;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.http.AbstractHttpSession;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.entity.mime.MIME.UTF8_CHARSET;

public class ResourceOwnerPasswordCredentialsBasicAuth extends AbstractOauthAuthenticator {

	public ResourceOwnerPasswordCredentialsBasicAuth(AbstractHttpSession session) throws HttpAuthenticationException {
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

		if (session.getUsername() == null) {
			throw new ConfigurationException("username is required");
		}

		if (session.getPassword() != null) {
			throw new ConfigurationException("password is required");
		}
	}

	private String createAuthorizationHeaderValue() {
		String value = URLEncoder.encode(session.getClientId(), UTF8_CHARSET) +
				':' +
				URLEncoder.encode(session.getClientSecret(), UTF8_CHARSET);

		return "Basic " + Base64.encode(value.getBytes(UTF8_CHARSET));
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials) throws HttpAuthenticationException {
		List<BasicHeader> parameters = new ArrayList<>();
		parameters.add(new BasicHeader("grant_type", "password"));

		if (session.getScope() != null) {
			parameters.add(new BasicHeader("scope", session.getScope().replace(',', ' ')));
		}

		parameters.add(new BasicHeader("username", credentials.getUserPrincipal().getName()));
		parameters.add(new BasicHeader("password", credentials.getPassword()));

		HttpEntityEnclosingRequestBase request = createPostRequestWithForm(authorizationEndpoint, parameters);
		request.addHeader(HttpHeaders.AUTHORIZATION, createAuthorizationHeaderValue());

		return request;
	}
}
