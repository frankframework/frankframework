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

import java.util.Arrays;
import java.util.List;
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
	private String consumes = "ANY";
	private String produces = "ANY";
	private List<String> mediaTypes = Arrays.asList("XML", "JSON", "TEXT");

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		ApiServiceDispatcher.getInstance().registerServiceClient(this, getCleanPattern(), getMethod());
	}

	public void close() {
		ApiServiceDispatcher.getInstance().unregisterServiceClient(getCleanPattern(), getMethod());
		super.close();
	}

	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException {
		String response = super.processRequest(correlationId, message, requestContext);

//		System.out.println("message: " + message);
//		System.out.println("response: " + response);

		return response;
	}

	public Object getSpecialDefaultValue(String attributeName, Object defaultValue, Map<String, String> attributes) {
		return defaultValue;
	}

	public String getPhysicalDestinationName() {
		return "uriPattern: "+getUriPattern()+"; method: "+getMethod();
	}

	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}
	public String getUriPattern() {
		return uriPattern;
	}
	public String getCleanPattern() {
		String pattern = uriPattern;
		if(pattern.startsWith("/"))
			pattern = pattern.substring(1);

		if(pattern.endsWith("/"))
			pattern = pattern.substring(0, pattern.length()-1);

		return pattern.replaceAll("\\{.*?}", "*");
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

	public void setConsumes(String consumes) throws ConfigurationException {
		if(mediaTypes.contains(consumes))
			this.consumes = consumes;
		else
			throw new ConfigurationException("Unknown mediatype ["+consumes+"]");
	}
	public String getConsumes() {
		return consumes;
	}

	public void setProduces(String produces) throws ConfigurationException {
		if(mediaTypes.contains(produces))
			this.produces = produces;
		else
			throw new ConfigurationException("Unknown mediatype ["+produces+"]");
	}
	public String getProduces() {
		return produces;
	}

	public boolean getGenerateEtag() {
		return true;
	}
}
