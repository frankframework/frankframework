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

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * This class registers dispatches requests to the proper registered ApiListeners.
 * The dispatcher does not handle nor does it process messages!
 * 
 * @author Niels Meijer
 *
 */
public class ApiServiceDispatcher {

	private Logger log = LogUtil.getLogger(this);
	private SortedMap<String, ApiDispatchConfig> patternClients = new TreeMap<String, ApiDispatchConfig>(new ApiUriComparator());
	private static ApiServiceDispatcher self = null;

	public static synchronized ApiServiceDispatcher getInstance() {
		if( self == null ) {
			self = new ApiServiceDispatcher();
		}
		return self;
	}

	public ApiDispatchConfig findConfigForUri(String uri) {
		ApiDispatchConfig config = null;

		String uriSegments[] = uri.split("/");

		for (Iterator<String> it = patternClients.keySet().iterator(); it.hasNext();) {
			String uriPattern = it.next();
			log.trace("comparing uri ["+uri+"] to pattern ["+uriPattern+"]");

			String patternSegments[] = uriPattern.split("/");
			if(patternSegments.length != uriSegments.length)
				continue;

			int matches = 0;
			for (int i = 0; i < patternSegments.length; i++) {
				if(patternSegments[i].equals(uriSegments[i]) || patternSegments[i].equals("*")) {
					matches++;
				}
				else {
					break;
				}
			}
			if(matches == uriSegments.length) {
				config = patternClients.get(uriPattern);
				break;
			}
		}

		return config;
	}

	public void registerServiceClient(ApiListener listener) throws ConfigurationException {
		String uriPattern = listener.getCleanPattern();
		String method = listener.getMethod();

		ApiDispatchConfig dispatchConfig = null;
		if(patternClients.containsKey(uriPattern))
			dispatchConfig = patternClients.get(uriPattern);
		else
			dispatchConfig = new ApiDispatchConfig(uriPattern);

		dispatchConfig.register(method, listener);

		patternClients.put(uriPattern, dispatchConfig);
		log.trace("ApiServiceDispatcher successfully registered uriPattern ["+uriPattern+"] method ["+method+"]");
	}

	public void unregisterServiceClient(ApiListener listener) {
		String method = listener.getMethod();
		String uriPattern = listener.getCleanPattern();

		ApiDispatchConfig dispatchConfig = patternClients.get(uriPattern);
		dispatchConfig.destroy(method);

		log.trace("ApiServiceDispatcher successfully unregistered uriPattern ["+uriPattern+"] method ["+method+"]");
	}
	
	public SortedMap<String, ApiDispatchConfig> getPatternClients() {
		return patternClients;
	}

}
