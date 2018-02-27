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
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.http.PushingListenerAdapter;

/**
 * The new and improved RESTful Listener now available for use!
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

	private String consumes = "ANY";
	private String produces = "ANY";
	private List<String> mediaTypes = Arrays.asList("ANY", "XML", "JSON", "TEXT", "MULTIPART");

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

		if(!mediaTypes.contains(getProduces()))
			throw new ConfigurationException("Unknown mediatype ["+produces+"]");

		if(!mediaTypes.contains(getConsumes()))
			throw new ConfigurationException("Unknown mediatype ["+consumes+"]");

		if(getAuthenticationMethod() != null && !authenticationMethods.contains(getAuthenticationMethod()))
			throw new ConfigurationException("Unknown authenticationMethod ["+authenticationMethod+"]");
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		try {
			ApiServiceDispatcher.getInstance().registerServiceClient(this);
		} catch (ConfigurationException e) {
			throw new ListenerException(e);
		}
	}
	
	@Override
	public void close() {
		super.close();
		ApiServiceDispatcher.getInstance().unregisterServiceClient(this);
	}

	@Override
	public String processRequest(String correlationId, String message, Map requestContext) throws ListenerException {
		String result = super.processRequest(correlationId, message, requestContext);
		if(result != null && result.isEmpty())
			return null;
		else
			return result;
	}

	public String getPhysicalDestinationName() {
		String destinationName = "uriPattern: "+getCleanPattern()+"; method: "+getMethod();
		if(!getConsumes().equals("ANY"))
			destinationName += "; consumes: "+getConsumes();
		if(!getProduces().equals("ANY"))
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

	public void setConsumes(String consumes) {
		this.consumes = consumes.toUpperCase();
	}
	public String getConsumes() {
		return consumes;
	}
	public boolean isConsumable(String contentType) {
		if(getConsumes().equals("ANY"))
			return true;

		String mediaType = "text/plain";
		if(getConsumes().equals("XML"))
			mediaType = "application/xml";
		else if(getConsumes().equals("JSON"))
			mediaType = "application/json";
		else if(getConsumes().equals("MULTIPART"))
			mediaType = "multipart/"; //There are different multipart contentTypes, see: https://msdn.microsoft.com/en-us/library/ms527355(v=exchg.10).aspx

		return contentType.contains(mediaType);
	}

	public void setProduces(String produces) {
		this.produces = produces.toUpperCase();
	}
	public String getProduces() {
		return produces;
	}
	public String getContentType() {
		String contentType = "*/*";
		if(getProduces().equals("XML"))
			contentType = "application/xml";
		else if(getProduces().equals("JSON"))
			contentType = "application/json";
		else if(getProduces().equals("TEXT"))
			contentType = "text/plain";
		return contentType;
	}

	public void setUpdateEtag(boolean updateEtag) {
		this.updateEtag = updateEtag;
	}
	public boolean getUpdateEtag() {
		return updateEtag;
	}
}
