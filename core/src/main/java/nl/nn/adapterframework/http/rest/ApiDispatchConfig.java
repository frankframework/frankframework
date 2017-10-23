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
package nl.nn.adapterframework.http.rest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class ApiDispatchConfig {
	private String uriPattern;
	private Map<String, ApiListener> methods = new HashMap<String, ApiListener>();

	public ApiDispatchConfig(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public void register(String method, ApiListener listener) throws ConfigurationException {
		method = method.toUpperCase();
		if(methods.containsKey(method))
			throw new ConfigurationException("ApiListener for uriPattern ["+uriPattern+"] method ["+method+"] has already registered");
		methods.put(method, listener);
	}

	public ApiListener getApiListener(String method) {
		method = method.toUpperCase();
		if(!methods.containsKey(method))
			return null;

		return methods.get(method);
	}

	public void destroy(String method) {
		method = method.toUpperCase();
		methods.remove(method);
	}

	public Set<String> getMethods() {
		return methods.keySet();
	}

	public boolean hasMethod(String method) {
		method = method.toUpperCase();
		return methods.containsKey(method);
	}

	public String getUriPattern() {
		return uriPattern;
	}
}
