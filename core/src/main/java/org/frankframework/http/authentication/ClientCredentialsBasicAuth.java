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

import static org.apache.http.entity.mime.MIME.UTF8_CHARSET;

import java.net.URLEncoder;
import java.util.List;

import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.auth.Credentials;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import com.nimbusds.jose.util.Base64;

import org.frankframework.http.AbstractHttpSession;

public class ClientCredentialsBasicAuth extends AbstractClientCredentials {

	public ClientCredentialsBasicAuth(AbstractHttpSession session) throws HttpAuthenticationException {
		super(session);
	}

	private String createAuthorizationHeaderValue() {
		String value = URLEncoder.encode(session.getClientId(), UTF8_CHARSET) +
				':' +
				URLEncoder.encode(session.getClientSecret(), UTF8_CHARSET);

		return "Basic " + Base64.encode(value.getBytes(UTF8_CHARSET));
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException {
		HttpEntityEnclosingRequestBase request = super.createRequest(credentials, parameters);
		request.addHeader(HttpHeaders.AUTHORIZATION, createAuthorizationHeaderValue());

		return request;
	}
}
