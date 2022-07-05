/*
   Copyright 2017-2019 WeAreFrank!

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

package nl.nn.adapterframework.http.rest;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;

public class ApiDispatchConfig {
	private String uriPattern;
	private Map<HttpMethod, ApiListener> methods = new ConcurrentHashMap<>();

	public ApiDispatchConfig(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public synchronized void register(HttpMethod method, ApiListener listener) throws ListenerException {
		if(methods.containsKey(method))
			throw new ListenerException("ApiListener for uriPattern ["+uriPattern+"] method ["+method+"] has already registered");
		methods.put(method, listener);
	}

	public ApiListener getApiListener(HttpMethod method) {
		if(!methods.containsKey(method))
			return null;

		return methods.get(method);
	}

	public void remove(HttpMethod method) {
		methods.remove(method);
	}

	public Set<HttpMethod> getMethods() {
		TreeSet<HttpMethod> sortedSet = new TreeSet<>(methods.keySet());
		return Collections.unmodifiableSet(sortedSet);
	}

	public void clear() {
		methods.clear();
	}

	public boolean hasMethod(HttpMethod method) {
		return methods.containsKey(method);
	}

	public String getUriPattern() {
		return uriPattern;
	}

	@Override
	public String toString() {
		return this.getClass().toString() + " methods" + getMethods().toString() + " uriPattern["+getUriPattern()+"]";
	}
}
