/*
Copyright 2017 - 2019 Integration Partners B.V.

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
import java.util.concurrent.ConcurrentHashMap;

import nl.nn.adapterframework.core.ListenerException;

public class ApiDispatchConfig {
	private String uriPattern;
	private Map<String, ApiListener> methods = new ConcurrentHashMap<String, ApiListener>();

	public ApiDispatchConfig(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public synchronized void register(String method, ApiListener listener) throws ListenerException {
		method = method.toUpperCase();
		if(methods.containsKey(method))
			throw new ListenerException("ApiListener for uriPattern ["+uriPattern+"] method ["+method+"] has already registered");
		methods.put(method, listener);
	}

	public ApiListener getApiListener(String method) {
		method = method.toUpperCase();
		if(!methods.containsKey(method))
			return null;

		return methods.get(method);
	}

	public void remove(String method) {
		method = method.toUpperCase();
		methods.remove(method);
	}

	public Set<String> getMethods() {
		return Collections.unmodifiableSet(methods.keySet());
	}

	public void clear() {
		methods.clear();
	}

	public boolean hasMethod(String method) {
		method = method.toUpperCase();
		return methods.containsKey(method);
	}

	public String getUriPattern() {
		return uriPattern;
	}

	@Override
	public String toString() {
		return this.getClass().toString() + " methods" + 
				getMethods().toString() + " uriPattern["+getUriPattern()+"]";
	}
}
