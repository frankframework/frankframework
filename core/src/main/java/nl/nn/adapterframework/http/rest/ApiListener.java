/*
   Copyright 2017-2025 WeAreFrank!

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

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.nimbusds.jose.proc.SecurityContext;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.Default;
import nl.nn.adapterframework.http.HttpSenderBase;
import nl.nn.adapterframework.http.PushingListenerAdapter;
import nl.nn.adapterframework.jwt.JwtValidator;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.adapterframework.lifecycle.servlets.ServletConfiguration;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.receivers.ReceiverAware;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.StringUtil;

// TODO: Link to https://swagger.io/specification/ when anchors are supported by the Frank!Doc.
/**
 * Listener that allows a {@link Receiver} to receive messages as a REST webservice.
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
	private @Getter boolean updateEtag = AppConstants.getInstance().getBoolean("api.etag.enabled", false);
	private @Getter String operationId;

	private @Getter HttpMethod method = HttpMethod.GET;
	public enum HttpMethod {
		GET,PUT,POST,PATCH,DELETE,OPTIONS;
	}

	private @Getter AuthenticationMethods authenticationMethod = AuthenticationMethods.NONE;
	private List<String> authenticationRoles = null;

	private @Getter MediaTypes consumes = MediaTypes.ANY;
	private @Getter MediaTypes produces = MediaTypes.ANY;
	private @Getter MimeType contentType;
	private String multipartBodyName = null;

	private @Getter @Setter Receiver<String> receiver;

	private @Getter String messageIdHeader = AppConstants.getInstance(getConfigurationClassLoader()).getString("apiListener.messageIdHeader", HttpSenderBase.MESSAGE_ID_HEADER);
	private @Getter String correlationIdHeader = AppConstants.getInstance(getConfigurationClassLoader()).getString("apiListener.correlationIdHeader", HttpSenderBase.CORRELATION_ID_HEADER);
	private @Getter String headerParams = null;
	private @Getter String contentDispositionHeaderSessionKey;
	private @Getter String charset = null;

	// for jwt validation
	private @Getter String requiredIssuer = null;
	private @Getter String jwksUrl = null;
	private @Getter String jwtHeader = HttpHeaders.AUTHORIZATION;
	private @Getter String requiredClaims = null;
	private @Getter String exactMatchClaims = null;
	private @Getter String anyMatchClaims = null;
	private @Getter String roleClaim;

	private @Getter String principalNameClaim = "sub";
	private @Getter(onMethod = @__(@Override)) String physicalDestinationName = null;

	private @Getter JwtValidator<SecurityContext> jwtValidator;
	private @Setter ServletManager servletManager;

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

		if(getAuthenticationMethod() == AuthenticationMethods.JWT) {
			if(StringUtils.isEmpty(getJwksUrl())){
				throw new ConfigurationException("jwksUrl cannot be empty");
			}

			// validate if these attributes are configured as valid key/value pairs, i.e: "appid=abc,appid=xyz" to prevent exceptions.
			validateClaimAttribute(anyMatchClaims, "anyMatchClaims");
			validateClaimAttribute(exactMatchClaims, "exactMatchClaims");
		}

		contentType = produces.getMimeType(charset);

		buildPhysicalDestinationName();
	}

	private void validateClaimAttribute(String claimAttributeToValidate, String claimAttributeName) throws ConfigurationException {
		if(StringUtils.isNotEmpty(claimAttributeToValidate)){
			List<String> invalidClaims = StringUtil.splitToStream(claimAttributeToValidate)
					.filter(claim -> StringUtil.split(claim, "=").size() != 2)
					.collect(Collectors.toList());

			if(!invalidClaims.isEmpty()){
				throw new ConfigurationException("[" + invalidClaims + "] are not a valid key/value pairs for [" + claimAttributeName + "].");
			}
		}
	}


	@Override
	public void open() throws ListenerException {
		ApiServiceDispatcher.getInstance().registerServiceClient(this);
		if(getAuthenticationMethod() == AuthenticationMethods.JWT) {
			try {
				jwtValidator = new JwtValidator<>();
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

	@Override
	public Message processRequest(Message message, PipeLineSession session) throws ListenerException {
		Message result = super.processRequest(message, session);

		//Return null when super.processRequest() returns an empty string
		if(Message.isEmpty(result)) {
			return null;
		}

		return result;
	}

	private void buildPhysicalDestinationName() {
		StringBuilder builder = new StringBuilder("uriPattern: ");
		if(servletManager != null) {
			ServletConfiguration config = servletManager.getServlet(ApiListenerServlet.class.getSimpleName());
			if(config != null) {
				if(config.getUrlMapping().size() == 1) {
					builder.append(config.getUrlMapping().get(0));
				} else {
					builder.append(config.getUrlMapping());
				}
			}
		}
		builder.append(getUriPattern());
		builder.append("; method: ").append(getMethod());

		if(!MediaTypes.ANY.equals(consumes)) {
			builder.append("; consumes: ").append(getConsumes());
		}
		if(!MediaTypes.ANY.equals(produces)) {
			builder.append("; produces: ").append(getProduces());
		}

		this.physicalDestinationName = builder.toString();
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
	 * Match request 'Content-Type' (eg. on POST) to consumes enum to see if the listener accepts the message
	 */
	public boolean isConsumable(@Nullable String contentType) {
		return consumes.includes(contentType);
	}

	/**
	 * Match request 'Accept' header to produces enum to see if the client accepts the message
	 */
	public boolean accepts(@Nullable String acceptHeader) {
		return produces.accepts(acceptHeader);
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
	public void setConsumes(@Nonnull MediaTypes value) {
		this.consumes = value;
	}

	/**
	 * The specified contentType on response. When <code>ANY</code> the response will determine the content type based on the return data.
	 * @ff.default ANY
	 */
	public void setProduces(@Nonnull MediaTypes value) {
		this.produces = value;
	}

	/**
	 * The specified character encoding on the response contentType header. NULL or empty
	 * values will be ignored.
	 * @ff.default UTF-8
	 */
	public void setCharacterEncoding(String charset) {
		if(StringUtils.isNotBlank(charset)) {
			this.charset = charset;
		}
	}

	/**
	 * Automatically generate and validate etags
	 * @ff.default <code>false</code>, can be changed by setting the property <code>api.etag.enabled</code>.
	 */
	public void setUpdateEtag(boolean updateEtag) {
		this.updateEtag = updateEtag;
	}

	//TODO add authenticationType

	/**
	 * Enables security for this listener. If you wish to use the application servers authorisation roles [AUTHROLE], you need to enable them globally for all ApiListeners with the `servlet.ApiListenerServlet.securityRoles=IbisTester,IbisWebService` property
	 * @ff.default <code>NONE</code>
	 */
	public void setAuthenticationMethod(AuthenticationMethods authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

	/**
	 * Only active when AuthenticationMethod=AUTHROLE. Comma separated list of authorization roles which are granted for this service, eq. IbisTester,IbisObserver", ""})
	 */
	public void setAuthenticationRoles(String authRoles) {
		this.authenticationRoles = StringUtil.split(authRoles, ",;");
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
	 * Name of the header which contains the Message-Id.
	 */
	@Default(HttpSenderBase.MESSAGE_ID_HEADER)
	public void setMessageIdHeader(String messageIdHeader) {
		this.messageIdHeader = messageIdHeader;
	}

	/**
	 * Name of the header which contains the Correlation-Id.
	 */
	@Default(HttpSenderBase.CORRELATION_ID_HEADER)
	public void setCorrelationIdHeader(String correlationIdHeader) {
		this.correlationIdHeader = correlationIdHeader;
	}

	/**
	 * Unique string used to identify the operation. The id MUST be unique among all operations described in the OpenApi schema.
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

	/** Session key that provides the Content-Disposition header in the response */
	public void setContentDispositionHeaderSessionKey(String key) {
		this.contentDispositionHeaderSessionKey = key;
	}

	/** Issuer to validate JWT */
	public void setRequiredIssuer(String issuer) {
		this.requiredIssuer = issuer;
	}

	/** Keysource URL to validate JWT */
	public void setJwksURL(String string) {
		this.jwksUrl = string;
	}

	/** Header to extract JWT from */
	public void setJwtHeader(String string) {
		this.jwtHeader = string;
	}

	/** Comma separated list of required claims */
	public void setRequiredClaims(String string) {
		this.requiredClaims = string;
	}

	/** Comma separated key value pairs to exactly match with JWT payload. e.g. "sub=UnitTest, aud=test" */
	public void setExactMatchClaims(String string) {
		this.exactMatchClaims = string;
	}

	/** Comma separated key value pairs to one-of match with JWT payload. e.g. "appid=a,appid=b" */
	public void setAnyMatchClaims(String string) {
		this.anyMatchClaims = string;
	}

	/** Claim name which specifies the role */
	public void setRoleClaim(String roleClaim) {
		this.roleClaim = roleClaim;
	}

	/** Claim name which specifies the principal name (maps to GetPrincipalPipe) */
	public void setPrincipalNameClaim(String principalNameClaim) {
		this.principalNameClaim = principalNameClaim;
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
