/*
Copyright 2017 Integration Partners B.V.

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
package org.frankframework.http.rest;

import java.io.Serializable;

import org.frankframework.util.AppConstants;
import org.frankframework.util.TimeProvider;

public class ApiPrincipal implements Serializable {

	private static final long serialVersionUID = 1L;
	public long init = TimeProvider.nowAsMillis();
	public long ttl = AppConstants.getInstance().getInt("api.auth.token-ttl", 60 * 60 * 24 * 7) * 1000L;
	public long expires = 0;

	public String data = null;
	public String authorizationToken = null;

	public ApiPrincipal() {
		updateExpiry();
	}

	/**
	 * TimeToLive in seconds
	 * @param ttl
	 */
	public ApiPrincipal(int ttl) {
		this.ttl = ttl*1000L;
		updateExpiry();
	}

	public boolean isLoggedIn() {
		return expires - TimeProvider.nowAsMillis() >= 0;
	}

	public void updateExpiry() {
		this.expires = TimeProvider.nowAsMillis() + this.ttl;
	}

	public void setData(String data) {
		this.data = data;
	}

	public String getData() {
		return data;
	}

	public void setToken(String authorizationToken) {
		this.authorizationToken = authorizationToken;
	}

	public String getToken() {
		return authorizationToken;
	}
}
