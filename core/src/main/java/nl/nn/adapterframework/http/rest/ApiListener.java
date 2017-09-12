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

import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.HasSpecialDefaultValues;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.http.PushingListenerAdapter;

public class ApiListener extends PushingListenerAdapter implements HasPhysicalDestination, HasSpecialDefaultValues {

	private String uriPattern;
	private String method;
	private String authenticationMethod = null;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		ApiServiceDispatcher.getInstance().registerServiceClient(this, getUriPattern(), getMethod());
	}

	public void close() {
		ApiServiceDispatcher.getInstance().unregisterServiceClient(getUriPattern(), getMethod());
		super.close();
	}

	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException {
		String response = super.processRequest(correlationId, message, requestContext);

		System.out.println("message: " + message);
		System.out.println("response: " + response);

		return response;
	}

	public Object getSpecialDefaultValue(String attributeName, Object defaultValue, Map<String, String> attributes) {
		return defaultValue;
	}

	public String getPhysicalDestinationName() {
		return "uriPattern: "+getUriPattern()+"; method: "+getMethod();
	}

	public String getUriPattern() {
		return uriPattern;
	}
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}

	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}

	//TODO add authenticationType

	public void setAuthenticationMethod(String method) {
		if(method.equalsIgnoreCase("token")) {
			this.authenticationMethod = "token";
		}
	}

	public String getAuthenticationMethod() {
		return this.authenticationMethod;
	}

	public String getConsumes() {
		// TODO FIX THIS
		return "any"; //application/json
	}

	public String getProduces() {
		return "application/xml";
	}

	public boolean getGenerateEtag() {
		return true;
	}
}
