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

import org.frankframework.http.AbstractHttpSession;

public class ClientCredentialsQueryParameters extends AbstractClientCredentials {
	public ClientCredentialsQueryParameters(AbstractHttpSession session) throws HttpAuthenticationException {
		super(session);
	}

	@Override
	protected HttpEntityEnclosingRequestBase createRequest(Credentials credentials, List<NameValuePair> parameters) throws HttpAuthenticationException {
		parameters.add(new BasicNameValuePair("client_secret", this.clientSecret));
		parameters.add(new BasicNameValuePair("client_id", this.clientId));

		return super.createRequest(credentials, parameters);
	}
}
