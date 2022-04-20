/*
   Copyright 2017-2022 WeAreFrank!

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

import lombok.Getter;
import lombok.Setter;
import com.nimbusds.jose.proc.SecurityContext;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.http.PushingListenerAdapter;
import nl.nn.adapterframework.jwt.JwtValidator;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.receivers.ReceiverAware;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;

// TODO: Link to https://swagger.io/specification/ when anchors are supported by the Frank!Doc.
/**
 * Listener that allows a {@link nl.nn.adapterframework.receivers.Receiver} to receive messages as a REST webservice.
 * Prepends the configured URI pattern with <code>api/</code>. The structure of REST messages is described
 * by OpenAPI specifications. The Frank!Framework generates an OpenAPI specification for each ApiListener and
 * also an OpenAPI specification for all ApiListeners in all configurations. You can
 * find them in the Frank!Console under main menu item Webservices, heading Available ApiListeners.
 * The generated OpenAPI specifications have <code>servers</code> and <code>paths</code> objects and
 * therefore they document the full URLs of the provided services. 
 * 
 * @author Niels Meijer
 *
 */
public class ApiListener extends PushingListenerAdapter implements HasPhysicalDestination, ReceiverAware<String> {

	private final @Getter(onMethod = @__(@Override)) String domain = "Http";
	private @Getter String uriPattern;
	private @Getter boolean updateEtag = true;
	private @Getter String operationId;

	private @Getter HttpMethod method = HttpMethod.GET;
	public enum HttpMethod {
		GET,PUT,POST,PATCH,DELETE,OPTIONS;
	}

	private @Getter AuthenticationMethods authenticationMethod = AuthenticationMethods.NONE;
	private List<String> authenticationRoles = null;

	private @Getter MediaTypes consumes = MediaTypes.ANY;
	private @Getter MediaTypes produces = MediaTypes.ANY;
	private @Getter ContentType producedContentType;
	private String multipartBodyName = null;

	private @Getter @Setter Receiver<String> receiver;

	private @Getter String messageIdHeader = AppConstants.getInstance(getConfigurationClassLoader()).getString("apiListener.messageIdHeader", "Message-Id");
	private @Getter String headerParams = null;
	private @Getter String charset = null;

	// for jwt validation
	private @Getter String requiredIssuer=null;
	private @Getter String jwksUrl=null;
	private @Getter String requiredClaims=null;
	private @Getter String exactMatchClaims=null;
	private @Getter String roleClaim;

	private @Getter JwtValidator<SecurityContext> jwtValidator;

	public enum AuthenticationMethods {
		NONE, COOKIE, HEADER, AUTHROLE, JWT;
	}

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		if(StringUtils.isEmpty(getUriPattern()))
			throw new ConfigurationException("uriPattern cannot be empty");

		if(getConsumes() != MediaTypes.ANY) {
			if(getMethod() == HttpMethod.GET)
				throw new ConfigurationException("cannot set consumes attribute when using method [GET]");
			if(getMethod() == HttpMethod.DELETE)
				throw new ConfigurationException("cannot set consumes attribute when using method [DELETE]");
		}

		if(getAuthenticationMethod() == AuthenticationMethods.JWT && StringUtils.isEmpty(getJwksUrl())) {
			throw new ConfigurationException("jwksUrl cannot be empty");
		}

		producedContentType = new ContentType(produces);
		if(charset != null) {
			producedContentType.setCharset(charset);
		}
	}

	@Override
	public void open() throws ListenerException {
		ApiServiceDispatcher.getInstance().registerServiceClient(this);
		if(getAuthenticationMethod() == AuthenticationMethods.JWT) {
			try {
				jwtValidator = new JwtValidator<SecurityContext>();
				jwtValidator.init(getJwksUrl(), getRequiredIssuer());
			} catch (Exception e) {
				throw new ListenerException("unable to initialize jwtSecurityHandler", e);
			}
		}
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
		String destinationName = "uriPattern: "+getUriPattern()+"; method: "+getMethod();
		if(!MediaTypes.ANY.equals(consumes))
			destinationName += "; consumes: "+getConsumes();
		if(!MediaTypes.ANY.equals(produces))
			destinationName += "; produces: "+getProduces();

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

	/** 
	 * HTTP method to listen to
	 * @ff.default GET
	 */
	public void setMethod(HttpMethod method) {
		this.method = method;
		if(this.method == HttpMethod.OPTIONS) {
			throw new IllegalArgumentException("method OPTIONS should not be added manually");
		}
	}

	/** 
	 * URI pattern to register this listener on, eq. `/my-listener/{something}/here`
	 * @ff.mandatory
	 */
	public void setUriPattern(String uriPattern) {
		if(!uriPattern.startsWith("/"))
			uriPattern = "/" + uriPattern;

		if(uriPattern.endsWith("/"))
			uriPattern = uriPattern.substring(0, uriPattern.length()-1);

		this.uriPattern = uriPattern;
	}

	/** 
	 * The required contentType on requests, if it doesn't match the request will fail
	 * @ff.default ANY
	 */
	public void setConsumes(MediaTypes value) {
		this.consumes = value;
	}

	/** 
	 * The specified contentType on response. When <code>ANY</code> the response will determine the content type based on the return data.
	 * @ff.default ANY
	 */
	public void setProduces(MediaTypes value) {
		this.produces = value;
	}

	/** 
	 * The specified character encoding on the response contentType header
	 * @ff.default UTF-8
	 */
	public void setCharacterEncoding(String charset) {
		if(StringUtils.isNotEmpty(charset)) {
			this.charset = charset;
		}
	}
	public String getCharacterEncoding() {
		return charset;
	}

	/** 
	 * Automatically generate and validate etags
	 * @ff.default <code>true</code>
	 */
	public void setUpdateEtag(boolean updateEtag) {
		this.updateEtag = updateEtag;
	}

	//TODO add authenticationType

	/** 
	 * Enables security for this listener. If you wish to use the application servers authorisation roles [AUTHROLE], you need to enable them globally for all ApiListeners with the `servlet.ApiListenerServlet.securityroles=ibistester,ibiswebservice` property
	 * @ff.default <code>NONE</code>
	 */
	public void setAuthenticationMethod(AuthenticationMethods authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

	/** 
	 * Only active when AuthenticationMethod=AUTHROLE. Comma separated list of authorization roles which are granted for this service, eq. IbisTester,IbisObserver", ""})
	 */
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

	/** 
	 * Specify the form-part you wish to enter the pipeline
	 * @ff.default name of the first form-part
	 */
	public void setMultipartBodyName(String multipartBodyName) {
		this.multipartBodyName = multipartBodyName;
	}
	public String getMultipartBodyName() {
		if(StringUtils.isNotEmpty(multipartBodyName)) {
			return multipartBodyName;
		}
		return null;
	}

	/** 
	 * Name of the header which contains the message-id
	 * @ff.default message-id
	 */
	public void setMessageIdHeader(String messageIdHeader) {
		this.messageIdHeader = messageIdHeader;
	}

	/** 
	 * Unique string used to identify the operation. The id MUST be unique among all operations described in the OpenApi schema
	 */
	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}

	/** 
	 * Comma separated list of parameters passed as http header. Parameters will be stored in 'headers' sessionkey.
	 */
	public void setHeaderParams(String headerParams) {
		this.headerParams = headerParams;
	}

	/** issuer to validate jwt */
	public void setRequiredIssuer(String issuer) {
		this.requiredIssuer = issuer;
	}

	/** keysource url to validate jwt */
	public void setJwksURL(String string) {
		this.jwksUrl = string;
	}

	/** comma separated list of required claims */
	public void setRequiredClaims(String string) {
		this.requiredClaims = string;
	}

	/** comma separated key value pairs to match with jwt payload. e.g. "sub=UnitTest, aud=test" */
	public void setExactMatchClaims(String string) {
		this.exactMatchClaims = string;
	}

	/** claim name which specifies the role */
	public void setRoleClaim(String roleClaim) {
		this.roleClaim = roleClaim;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append(getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()));
		builder.append(" uriPattern["+getUriPattern()+"]");
		builder.append(" produces["+getProduces()+"]");
		builder.append(" consumes["+getConsumes()+"]");
		builder.append(" messageIdHeader["+getMessageIdHeader()+"]");
		builder.append(" updateEtag["+isUpdateEtag()+"]");
		return builder.toString();
	}

}
