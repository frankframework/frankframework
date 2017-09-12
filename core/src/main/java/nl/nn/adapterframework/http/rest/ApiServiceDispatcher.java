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
import nl.nn.adapterframework.receivers.ServiceClient;
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

	public void registerServiceClient(ServiceClient listener, String uriPattern, String method) throws ConfigurationException {
		String cleanPattern = cleanUriPattern(uriPattern);

		if (!(listener instanceof ApiListener))
			throw new ConfigurationException("ApiServiceDispatcher tried to register serviceClient ["+listener.getClass().toString()+"] expecting [" + nl.nn.adapterframework.http.rest.ApiListener.class.toString() + "]");
		ApiListener apiListener = (ApiListener) listener;

		ApiDispatchConfig dispatchConfig = null;
		if(patternClients.containsKey(cleanPattern))
			dispatchConfig = patternClients.get(cleanPattern);
		else
			dispatchConfig = new ApiDispatchConfig(cleanPattern);

		dispatchConfig.register(method, apiListener);

		patternClients.put(cleanPattern, dispatchConfig);
		log.trace("ApiServiceDispatcher successfully registered uriPattern ["+cleanPattern+"] method ["+method+"]");
	}

	public void unregisterServiceClient(String uriPattern, String method) {
		String cleanPattern = cleanUriPattern(uriPattern);

		ApiDispatchConfig dispatchConfig = patternClients.get(cleanPattern);
		dispatchConfig.destroy(method);

		if(dispatchConfig.getMethods().size() == 0)
			patternClients.remove(cleanPattern);
		log.trace("ApiServiceDispatcher successfully unregistered uriPattern ["+cleanPattern+"] method ["+method+"]");
	}

	private String cleanUriPattern(String uriPattern) {
		if(uriPattern.startsWith("/"))
			uriPattern = uriPattern.substring(1);

		if(uriPattern.endsWith("/"))
			uriPattern = uriPattern.substring(0, uriPattern.length()-1);

		return uriPattern.replaceAll("\\{.*?}", "*");
	}
}
