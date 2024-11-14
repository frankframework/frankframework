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

import org.apache.http.auth.Credentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.message.BasicHeader;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.http.AbstractHttpSession;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import static org.apache.http.entity.mime.MIME.UTF8_CHARSET;

public class ClientCredentialsBasicAuth extends AbstractOauthAuthenticator {

	public ClientCredentialsBasicAuth(AbstractHttpSession session) throws HttpAuthenticationException {
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

		if (session.getUsername() != null) {
			ConfigurationWarnings.add(session, log, "Username should not be set");
		}

		if (session.getPassword() != null) {
			ConfigurationWarnings.add(session, log, "Password should not be set");
		}
	}

	private String createAuthorizationHeaderValue() {
		String sb = URLEncoder.encode(session.getClientId(), UTF8_CHARSET) +
				':' +
				URLEncoder.encode(session.getClientSecret(), UTF8_CHARSET);

		return "Basic " + Base64.encode(sb.getBytes(UTF8_CHARSET));
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials) {
		List<BasicHeader> parameters = new ArrayList<>();
		parameters.add(new BasicHeader("grant_type", "client_credentials"));

		if (session.getScope() != null) {
			parameters.add(new BasicHeader("scope", session.getScope().replace(',', ' ')));
		}

		try {
			UrlEncodedFormEntity body = new UrlEncodedFormEntity(parameters, "utf-8");

			HttpPost request = new HttpPost(authorizationEndpoint);
			request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			request.setEntity(body);
			request.addHeader("Authorization", createAuthorizationHeaderValue());

			return request;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
