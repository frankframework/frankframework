/*
Copyright 2017-2020 WeAreFrank!

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.http.PushingListenerAdapter;
import nl.nn.adapterframework.receivers.ReceiverAware;
import nl.nn.adapterframework.util.AppConstants;

/**
 * 
 * @author Niels Meijer
 *
 */
public class ApiListener extends PushingListenerAdapter<String> implements HasPhysicalDestination, ReceiverAware<String> {

	private String uriPattern;
	private boolean updateEtag = true;

	private String method;
	private List<String> methods = Arrays.asList("GET", "PUT", "POST", "DELETE");

	private AuthenticationMethods authenticationMethod = AuthenticationMethods.NONE;
	private List<String> authenticationRoles = null;

	private MediaTypes consumes = MediaTypes.ANY;
	private MediaTypes produces = MediaTypes.ANY;
	private ContentType producedContentType;
	private String multipartBodyName = null;

	private IReceiver<String> receiver;

	private ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private String messageIdHeader = AppConstants.getInstance(configurationClassLoader).getString("apiListener.messageIdHeader", "Message-Id");
	private String charset = null;

	public enum AuthenticationMethods {
		NONE, COOKIE, HEADER, AUTHROLE;
	}

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		if(StringUtils.isEmpty(getUriPattern()))
			throw new ConfigurationException("uriPattern cannot be empty");

		if(!getConsumesEnum().equals(MediaTypes.ANY)) {
			if(getMethod().equals("GET"))
				throw new ConfigurationException("cannot set consumes attribute when using method [GET]");
			if(getMethod().equals("DELETE"))
				throw new ConfigurationException("cannot set consumes attribute when using method [DELETE]");
		}
		if(!methods.contains(getMethod()))
			throw new ConfigurationException("Method ["+method+"] not yet implemented, supported methods are "+methods.toString()+"");

		producedContentType = new ContentType(produces);
		if(charset != null) {
			producedContentType.setCharset(charset);
		}
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

	@Override
	public String getPhysicalDestinationName() {
		String destinationName = "uriPattern: "+getCleanPattern()+"; method: "+getMethod();
		if(!MediaTypes.ANY.equals(consumes))
			destinationName += "; consumes: "+getConsumesEnum().name();
		if(!MediaTypes.ANY.equals(produces))
			destinationName += "; produces: "+getProducesEnum().name();

		return destinationName;
	}

	/**
	 * returns the clear pattern, replaces everything between <code>{}</code> to <code>*</code>
	 * @return null if no pattern is found
	 */
	public String getCleanPattern() {
		String pattern = getUriPattern();
		if(StringUtils.isEmpty(pattern))
			return null;

		if(pattern.startsWith("/"))
			pattern = pattern.substring(1);

		if(pattern.endsWith("/"))
			pattern = pattern.substring(0, pattern.length()-1);

		return pattern.replaceAll("\\{.*?}", "*");
	}

	/**
	 * Match request ContentType to consumes enum to see if the listener accepts the message
	 */
	public boolean isConsumable(String contentType) {
		return consumes.isConsumable(contentType);
	}

	/**
	 * Match accept header to produces enum to see if the client accepts the message
	 */
	public boolean accepts(String acceptHeader) {
		return produces.equals(MediaTypes.ANY) || acceptHeader.contains("*/*") || acceptHeader.contains(produces.getContentType());
	}

	public String getContentType() {
		return producedContentType.getContentType();
	}

	@IbisDoc({"1", "HTTP method eq. GET POST PUT DELETE", ""})
	public void setMethod(String method) {
		this.method = method.toUpperCase();
	}
	public String getMethod() {
		return method;
	}

	@IbisDoc({"2", "uri pattern to register this listener on, eq. `/my-listener/{something}/here`", ""})
	public void setUriPattern(String uriPattern) {
		this.uriPattern = uriPattern;
	}
	public String getUriPattern() {
		return uriPattern;
	}

	@IbisDoc({"3", "the specified contentType on requests, if it doesn't match the request will fail", "ANY"})
	public void setConsumes(String value) {
		String consumes = null;
		if(StringUtils.isEmpty(value))
			consumes = "ANY";
		else
			consumes = value.toUpperCase();

		this.consumes = MediaTypes.valueOf(consumes);
	}
	public MediaTypes getConsumesEnum() {
		return consumes;
	}

	@IbisDoc({"4", "the specified contentType on response", "ANY"})
	public void setProduces(String value) {
		String produces = null;
		if(StringUtils.isEmpty(value))
			produces = "ANY";
		else
			produces = value.toUpperCase();

		this.produces = MediaTypes.valueOf(produces);
	}
	public MediaTypes getProducesEnum() {
		return produces;
	}

	@IbisDoc({"5", "sets the specified character encoding on the response contentType header", "UTF-8"})
	public void setCharacterEncoding(String charset) {
		if(StringUtils.isNotEmpty(charset)) {
			this.charset = charset;
		}
	}
	public String getCharacterEncoding() {
		return charset;
	}

	@IbisDoc({"6", "automatically generate and validate etags", "true"})
	public void setUpdateEtag(boolean updateEtag) {
		this.updateEtag = updateEtag;
	}
	public boolean getUpdateEtag() {
		return updateEtag;
	}

	//TODO add authenticationType

	@IbisDoc({"7", "enables security for this listener, must be one of [NONE, COOKIE, HEADER, AUTHROLE]. If you wish to use the application servers authorisation roles [AUTHROLE], you need to enable them globally for all ApiListeners with the `servlet.ApiListenerServlet.securityroles=ibistester,ibiswebservice` property", "NONE"})
	public void setAuthenticationMethod(String authenticationMethod) throws ConfigurationException {
		try {
			this.authenticationMethod = AuthenticationMethods.valueOf(authenticationMethod);
		}
		catch (IllegalArgumentException iae) {
			throw new ConfigurationException("Unknown authenticationMethod ["+authenticationMethod+"]. Must be one of "+ Arrays.asList(AuthenticationMethods.values()));
		}
	}

	public AuthenticationMethods getAuthenticationMethodEnum() {
		if(authenticationMethod == null) {
			authenticationMethod = AuthenticationMethods.NONE;
		}

		return this.authenticationMethod;
	}

	@IbisDoc({"8", "only active when AuthenticationMethod=AUTHROLE. comma separated list of authorization roles which are granted for this service, eq. ibistester,ibisobserver", ""})
	public void setAuthenticationRoles(String authRoles) {
		List<String> roles = new ArrayList<String>();
		if (StringUtils.isNotEmpty(authRoles)) {
			StringTokenizer st = new StringTokenizer(authRoles, ",;");
			while (st.hasMoreTokens()) {
				String authRole = st.nextToken();
				if(!roles.contains(authRole))
					roles.add(authRole);
			}
		}

		this.authenticationRoles = roles;
	}
	public List<String> getAuthenticationRoleList() {
		return authenticationRoles;
	}

	@IbisDoc({"9", "specify the form-part you wish to enter the pipeline", "name of the first form-part"})
	public void setMultipartBodyName(String multipartBodyName) {
		this.multipartBodyName = multipartBodyName;
	}
	public String getMultipartBodyName() {
		if(StringUtils.isNotEmpty(multipartBodyName))
			return multipartBodyName;

		return null;
	}

	@IbisDoc({"10", "name of the header which contains the message-id", "message-id"})
	public void setMessageIdHeader(String messageIdHeader) {
		this.messageIdHeader = messageIdHeader;
	}

	public String getMessageIdHeader() {
		return messageIdHeader;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" uriPattern["+getUriPattern()+"]");
		builder.append(" produces["+getProducesEnum().name()+"]");
		builder.append(" consumes["+getConsumesEnum().name()+"]");
		builder.append(" messageIdHeader["+getMessageIdHeader()+"]");
		builder.append(" updateEtag["+getUpdateEtag()+"]");
		return builder.toString();
	}

	@Override
	public void setReceiver(IReceiver<String> receiver) {
		this.receiver = receiver;
	}

	@Override
	public IReceiver<String> getReceiver() {
		return receiver;
	}
}
