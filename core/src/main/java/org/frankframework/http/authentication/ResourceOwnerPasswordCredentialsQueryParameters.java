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

import org.apache.http.auth.Credentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpPost;

import org.apache.http.message.BasicHeader;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.http.AbstractHttpSession;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class ResourceOwnerPasswordCredentialsQueryParameters extends AbstractOauthAuthenticator {

	public ResourceOwnerPasswordCredentialsQueryParameters(AbstractHttpSession session) throws HttpAuthenticationException {
		super(session);
	}

	@Override
	public void configure() throws ConfigurationException {

	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials) {
		List<BasicHeader> parameters = new ArrayList<>();
		parameters.add(new BasicHeader("grant_type", "password"));

		if (session.getScope() != null) {
			parameters.add(new BasicHeader("scope", session.getScope().replace(',', ' ')));
		}

		parameters.add(new BasicHeader("username", credentials.getUserPrincipal().getName()));
		parameters.add(new BasicHeader("password", credentials.getPassword()));

		parameters.add(new BasicHeader("client_id", session.getClientId()));
		parameters.add(new BasicHeader("client_secret", session.getClientSecret()));

		try {
			UrlEncodedFormEntity body = new UrlEncodedFormEntity(parameters, "utf-8");

			HttpPost request = new HttpPost(authorizationEndpoint);
			request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
			request.setEntity(body);

			return request;
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
