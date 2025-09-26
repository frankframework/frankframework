/*
   Copyright 2017-2025 WeAreFrank!

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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

public class ApiDispatchConfig {
	private final String uriPattern;
	private final Map<ApiListener.HttpMethod, ApiListener> methods = new ConcurrentHashMap<>();

	public ApiDispatchConfig(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public synchronized void register(ApiListener.HttpMethod method, ApiListener listener)  {
		if (methods.containsKey(method)) {
			throw new IllegalStateException("ApiListener for uriPattern [" + uriPattern + "] method [" + method + "] has already registered");
		}

		methods.put(method, listener);
	}

	public ApiListener getApiListener(ApiListener.HttpMethod method) {
		return methods.get(method);
	}

	public void remove(ApiListener.HttpMethod method) {
		methods.remove(method);
	}

	public Set<ApiListener.HttpMethod> getMethods() {
		TreeSet<ApiListener.HttpMethod> sortedSet = new TreeSet<>(methods.keySet());
		return Collections.unmodifiableSet(sortedSet);
	}

	public void clear() {
		methods.clear();
	}

	public boolean hasMethod(ApiListener.HttpMethod method) {
		return methods.containsKey(method);
	}

	public String getUriPattern() {
		return uriPattern;
	}

	@Override
	public String toString() {
		return this.getClass() + " methods" + getMethods() + " uriPattern[" + getUriPattern() + "]";
	}
}
