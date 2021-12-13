/*
Copyright 2017-2021 WeAreFrank!

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
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.http.PushingListenerAdapter;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.receivers.ReceiverAware;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.EnumUtils;

/**
 * 
 * @author Niels Meijer
 *
 */
public class ApiListener extends PushingListenerAdapter implements HasPhysicalDestination, ReceiverAware<String> {

	private String uriPattern;
	private boolean updateEtag = true;
	private String operationId;

	private HttpMethod method;
	public enum HttpMethod {
		GET,PUT,POST,PATCH,DELETE,OPTIONS;
	}

	private AuthenticationMethods authenticationMethod = AuthenticationMethods.NONE;
	private List<String> authenticationRoles = null;

	private MediaTypes consumes = MediaTypes.ANY;
	private MediaTypes produces = MediaTypes.ANY;
	private ContentType producedContentType;
	private String multipartBodyName = null;

	private Receiver<String> receiver;

	private String messageIdHeader = AppConstants.getInstance(getConfigurationClassLoader()).getString("apiListener.messageIdHeader", "Message-Id");
	private String headerParams = null;
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
			if(getMethodEnum() == HttpMethod.GET)
				throw new ConfigurationException("cannot set consumes attribute when using method [GET]");
			if(getMethodEnum() == HttpMethod.DELETE)
				throw new ConfigurationException("cannot set consumes attribute when using method [DELETE]");
		}

		producedContentType = new ContentType(produces);
		if(charset != null) {
			producedContentType.setCharset(charset);
		}
	}

	@Override
	public void open() throws ListenerException {
		ApiServiceDispatcher.getInstance().registerServiceClient(this);
		super.open();
	}

	@Override
	public void close() {
		super.close();
		ApiServiceDispatcher.getInstance().unregisterServiceClient(this);
	}

	public Message processRequest(String correlationId, Message message, PipeLineSession requestContext) throws ListenerException {
		Message result = super.processRequest(correlationId, message, requestContext);

		//Return null when super.processRequest() returns an empty string
		if(Message.isEmpty(result)) {
			return null;
		}

		return result;
	}

	@Override
	public String getPhysicalDestinationName() {
		String destinationName = "uriPattern: "+getUriPattern()+"; method: "+getMethodEnum();
		if(!MediaTypes.ANY.equals(consumes))
			destinationName += "; consumes: "+getConsumesEnum();
		if(!MediaTypes.ANY.equals(produces))
			destinationName += "; produces: "+getProducesEnum();

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

	public ContentType getContentType() {
		return producedContentType;
	}

	@IbisDoc({"1", "HTTP method to listen to", ""})
	public void setMethod(String method) {
		try {
			this.method = EnumUtils.parse(HttpMethod.class, method);
			if(this.method == HttpMethod.OPTIONS) {
				throw new IllegalArgumentException("method OPTIONS is default and should not be added manually");
			}
		} catch (IllegalArgumentException e) {
			List<HttpMethod> enums = EnumUtils.getEnumList(HttpMethod.class);
			enums.remove(HttpMethod.OPTIONS);
			throw new IllegalArgumentException("unknown httpMethod value ["+method+"]. Must be one of "+ enums, e);
		}
	}
	public HttpMethod getMethodEnum() {
		return method;
	}

	@IbisDoc({"2", "uri pattern to register this listener on, eq. `/my-listener/{something}/here`", ""})
	public void setUriPattern(String uriPattern) {
		if(!uriPattern.startsWith("/"))
			uriPattern = "/" + uriPattern;

		if(uriPattern.endsWith("/"))
			uriPattern = uriPattern.substring(0, uriPattern.length()-1);

		this.uriPattern = uriPattern;
	}
	public String getUriPattern() {
		return uriPattern;
	}

	@IbisDoc({"3", "the specified contentType on requests, if it doesn't match the request will fail", "ANY"})
	public void setConsumes(String value) {
		if(StringUtils.isNotEmpty(value)) {
			this.consumes = EnumUtils.parse(MediaTypes.class, value);
		}
	}
	public MediaTypes getConsumesEnum() {
		return consumes;
	}

	@IbisDoc({"4", "the specified contentType on response", "ANY"})
	public void setProduces(String value) {
		if(StringUtils.isNotEmpty(value)) {
			this.produces = EnumUtils.parse(MediaTypes.class, value);
		}
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
	public void setAuthenticationMethod(String authenticationMethod) {
		this.authenticationMethod = EnumUtils.parse(AuthenticationMethods.class, authenticationMethod);
	}

	public AuthenticationMethods getAuthenticationMethodEnum() {
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

	@IbisDoc({"11", "Unique string used to identify the operation. The id MUST be unique among all operations described in the OpenApi schema", ""})
	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}
	public String getOperationId() {
		return operationId;
	}

	@IbisDoc({"12", "Comma separated list of parameters passed as http header. Parameters will be stored in 'headers' sessionkey.", ""})
	public void setHeaderParams(String headerParams) {
		this.headerParams = headerParams;
	}
	public String getHeaderParams() {
		return headerParams;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" uriPattern["+getUriPattern()+"]");
		builder.append(" produces["+getProducesEnum()+"]");
		builder.append(" consumes["+getConsumesEnum()+"]");
		builder.append(" messageIdHeader["+getMessageIdHeader()+"]");
		builder.append(" updateEtag["+getUpdateEtag()+"]");
		return builder.toString();
	}

	@Override
	public void setReceiver(Receiver<String> receiver) {
		this.receiver = receiver;
	}

	@Override
	public Receiver<String> getReceiver() {
		return receiver;
	}
}
