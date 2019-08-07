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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.http.PushingListenerAdapter;

/**
 * 
 * @author Niels Meijer
 *
 */
public class ApiListener extends PushingListenerAdapter implements HasPhysicalDestination {

	private String uriPattern;
	private boolean updateEtag = true;

	private String method;
	private List<String> methods = Arrays.asList("GET", "PUT", "POST", "DELETE");

	private String authenticationMethod = null;
	private List<String> authenticationMethods = Arrays.asList("COOKIE", "HEADER");

	private MediaType consumes = MediaType.ANY;
	private MediaType produces = MediaType.ANY;

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	public void configure() throws ConfigurationException {
		if(!getConsumes().equals("ANY")) {
			if(getMethod().equals("GET"))
				throw new ConfigurationException("cannot set consumes attribute when using method [GET]");
			if(getMethod().equals("DELETE"))
				throw new ConfigurationException("cannot set consumes attribute when using method [DELETE]");
		}
		if(!methods.contains(getMethod()))
			throw new ConfigurationException("Method ["+method+"] not yet implemented, supported methods are "+methods.toString()+"");

		if(getAuthenticationMethod() != null && !authenticationMethods.contains(getAuthenticationMethod()))
			throw new ConfigurationException("Unknown authenticationMethod ["+authenticationMethod+"]");
	}

	@Override
	public void open() throws ListenerException {
		super.open();

		ApiServiceDispatcher.getInstance().registerServiceClient(this);
	}

	@Override
	public void close() {
		super.close();
		ApiServiceDispatcher.getInstance().unregisterServiceClient(this);
	}

	public String processRequest(String correlationId, String message, IPipeLineSession requestContext) throws ListenerException {
		String result = super.processRequest(correlationId, message, requestContext);

		//Return null when super.processRequest() returns an empty string
		if(result != null && result.isEmpty())
			return null;
		else
			return result;
	}

	public String getPhysicalDestinationName() {
		String destinationName = "uriPattern: "+getCleanPattern()+"; method: "+getMethod();
		if(!MediaType.ANY.equals(consumes))
			destinationName += "; consumes: "+getConsumes();
		if(!MediaType.ANY.equals(produces))
			destinationName += "; produces: "+getProduces();

		return destinationName;
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
		this.method = method.toUpperCase();
	}

	//TODO add authenticationType

	public void setAuthenticationMethod(String authenticationMethod) {
		this.authenticationMethod = authenticationMethod.toUpperCase();
	}
	public String getAuthenticationMethod() {
		return this.authenticationMethod;
	}

	public void setConsumes(String value) {
		String consumes = null;
		if(StringUtils.isEmpty(value))
			consumes = "ANY";
		else
			consumes = value.toUpperCase();

		this.consumes = MediaType.valueOf(consumes);
	}
	public String getConsumes() {
		return consumes.name();
	}

	public boolean isConsumable(String contentType) {
		return consumes.isConsumable(contentType);
	}

	public void setProduces(String value) {
		String produces = null;
		if(StringUtils.isEmpty(value))
			produces = "ANY";
		else
			produces = value.toUpperCase();

		this.produces = MediaType.valueOf(produces);
	}
	public String getProduces() {
		return produces.name();
	}

	public String getContentType() {
		return produces.getContentType();
	}

	public void setUpdateEtag(boolean updateEtag) {
		this.updateEtag = updateEtag;
	}
	public boolean getUpdateEtag() {
		return updateEtag;
	}

	@Override
	public String toString() {
		return this.getClass().toString() + "["+getPhysicalDestinationName()+"] "
				+ "contentType["+getContentType()+"] updateEtag["+getUpdateEtag()+"]";
	}
}
