/*
   Copyright 2017-2024 WeAreFrank!

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
package org.frankframework.http.rest;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.MimeType;

import com.nimbusds.jose.proc.SecurityContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.doc.Default;
import org.frankframework.http.AbstractHttpSender;
import org.frankframework.http.HttpEntityType;
import org.frankframework.http.PushingListenerAdapter;
import org.frankframework.jwt.JwtValidator;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.lifecycle.ServletManager;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ReceiverAware;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringUtil;

// TODO: Link to https://swagger.io/specification/ when anchors are supported by the Frank!Doc.

/**
 * Listener that allows a {@link Receiver} to receive messages as a REST webservice.
 * Prepends the configured URI pattern with <code>api/</code>. The structure of REST messages is described
 * by OpenAPI specifications. The Frank!Framework generates an OpenAPI specification for each ApiListener and
 * for all ApiListeners in all configurations combined. You can
 * find them in the Frank!Console under main menu item Webservices, heading Available ApiListeners.
 * <p>
 * The generated OpenAPI specifications have <code>servers</code> and <code>paths</code> objects and
 * therefore they document the full URLs of the provided services.
 * <p>
 * It is possible to automatically generate eTags over the listener result. This can be controlled by globally 
 * setting the property <code>api.etag.enabled</code> or by setting the attribute <code>updateEtag="true"</code>.
 * When enabled the listener will respond to the <code>If-Match</code>, <code>If-None-Match</code> headers and may return status code 304.
 * <p>
 * In order to enable eTags for multiple nodes you must configure Memcached to store the eTags.
 * The following properties will need to be set:
 * <ul>
 * <li><code>etag.cache.server=ip or hostname:port</code></li>
 * <li><code>etag.cache.type=memcached</code></li>
 * </ul>
 * In case authentication, is required the following application properties can be used:
 * <ul>
 * <li><code>etag.cache.username</code></li>
 * <li><code>etag.cache.password</code></li>
 * <li><code>etag.cache.authalias</code></li>
 * </ul>
 *
 * @author Niels Meijer
 */
public class ApiListener extends PushingListenerAdapter implements HasPhysicalDestination, ReceiverAware<Message> {

	private static final Pattern VALID_URI_PATTERN_RE = Pattern.compile("([^/]\\*|\\*[^/\\n])");

	private final @Getter String domain = "Http";
	private @Getter String uriPattern;
	private @Getter boolean updateEtag = AppConstants.getInstance().getBoolean("api.etag.enabled", false);
	private @Getter String operationId;

	private List<HttpMethod> methods = List.of(HttpMethod.GET);

	public enum HttpMethod {
		GET, PUT, POST, PATCH, DELETE, HEAD, OPTIONS;
	}

	private @Getter AuthenticationMethods authenticationMethod = AuthenticationMethods.NONE;
	private List<String> authenticationRoles = null;

	private @Getter MediaTypes consumes = MediaTypes.ANY;
	private @Getter MediaTypes produces = MediaTypes.ANY;
	private @Getter MimeType contentType;
	private String multipartBodyName = null;

	private @Getter @Setter Receiver<Message> receiver;

	private @Getter String messageIdHeader = AppConstants.getInstance(getConfigurationClassLoader()).getString("apiListener.messageIdHeader", AbstractHttpSender.MESSAGE_ID_HEADER);
	private @Getter String correlationIdHeader = AppConstants.getInstance(getConfigurationClassLoader()).getString("apiListener.correlationIdHeader", AbstractHttpSender.CORRELATION_ID_HEADER);
	private @Getter String headerParams = null;
	private @Getter String contentDispositionHeaderSessionKey;
	private @Getter String charset = null;

	// for jwt validation
	private @Getter String requiredIssuer = null;
	private @Getter String jwksUrl = null;
	private @Getter String jwtHeader = "Authorization";
	private @Getter String requiredClaims = null;
	private @Getter String exactMatchClaims = null;
	private @Getter String anyMatchClaims = null;
	private @Getter String roleClaim;

	private @Getter String principalNameClaim = "sub";
	private @Getter String physicalDestinationName = null;

	private @Getter JwtValidator<SecurityContext> jwtValidator;
	private @Setter ServletManager servletManager;

	private @Getter boolean responseAsMultipart;
	private @Getter HttpEntityType responseEntityType;
	private @Getter String responseMultipartXmlSessionKey;
	private @Getter String responseResultBodyPartName;
	private @Getter String responseMtomContentTransferEncoding;

	public enum AuthenticationMethods {
		NONE, COOKIE, HEADER, AUTHROLE, JWT;
	}

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getUriPattern()))
			throw new ConfigurationException("uriPattern cannot be empty");

		if (!isValidUriPattern(getCleanPattern())) {
			throw new ConfigurationException("uriPattern contains invalid wildcards");
		}

		if (getUriPattern().endsWith("/**")) {
			ConfigurationWarnings.add(this, log, "When using /** patterns in an ApiListener the generated OpenAPI spec might be incomplete", SuppressKeys.CONFIGURATION_VALIDATION, getReceiver().getAdapter());
		}

		if (getConsumes() != MediaTypes.ANY) {
			if (hasMethod(HttpMethod.GET))
				throw new ConfigurationException("cannot set consumes attribute when using method [GET]");
			if (hasMethod(HttpMethod.DELETE))
				throw new ConfigurationException("cannot set consumes attribute when using method [DELETE]");
		}
		if (getConsumes() == MediaTypes.DETECT) {
			throw new ConfigurationException("cannot set consumes attribute to [DETECT]");
		}

		if (getAuthenticationMethod() == AuthenticationMethods.JWT) {
			if (StringUtils.isEmpty(getJwksUrl())) {
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
		if (StringUtils.isNotEmpty(claimAttributeToValidate)) {
			List<String> invalidClaims = StringUtil.splitToStream(claimAttributeToValidate)
					.filter(claim -> StringUtil.split(claim, "=").size() != 2)
					.toList();

			if (!invalidClaims.isEmpty()) {
				String partialMessage = invalidClaims.size() == 1 ? "is not a valid key/value pair" : "are not valid key/value pairs";
				throw new ConfigurationException("[" + String.join(",", invalidClaims) + "] " + partialMessage + " for [" + claimAttributeName + "].");
			}
		}
	}

	@Override
	public void start() {
		ApiServiceDispatcher.getInstance().registerServiceClient(this);
		if (getAuthenticationMethod() == AuthenticationMethods.JWT) {
			try {
				jwtValidator = new JwtValidator<>();
				jwtValidator.init(getJwksUrl(), getRequiredIssuer());
			} catch (Exception e) {
				throw new LifecycleException("unable to initialize jwtSecurityHandler", e);
			}
		}
		super.start();
	}

	@Override
	public void stop() {
		super.stop();
		ApiServiceDispatcher.getInstance().unregisterServiceClient(this);
	}

	private void buildPhysicalDestinationName() {
		StringBuilder builder = new StringBuilder("uriPattern: ");
		if (servletManager != null) {
			ServletConfiguration config = servletManager.getServlet(ApiListenerServlet.class.getSimpleName());
			if (config != null) {
				List<String> modifiedUrls = config.getUrlMapping().stream()
						.map(url -> url.replace("/*", ""))
						.toList();
				if (modifiedUrls.size() == 1) {
					// Replace /api/* with /api. Doing this in ApiListenerServlet.getUrlMapping() is not possible.
					builder.append(modifiedUrls.get(0));
				} else {
					builder.append(modifiedUrls);
				}
			}
		}
		builder.append(getUriPattern());
		builder.append("; method: ").append(getAllMethods().stream().map(Enum::name).collect(Collectors.joining(",")));

		if (MediaTypes.ANY != consumes) {
			builder.append("; consumes: ").append(getConsumes());
		}
		if (MediaTypes.ANY != produces) {
			builder.append("; produces: ").append(getProduces());
		}

		this.physicalDestinationName = builder.toString();
	}

	private boolean hasMethod(HttpMethod method) {
		return methods.contains(method);
	}

	/**
	 * returns the clear pattern, replaces everything between <code>{}</code> to <code>*</code>
	 *
	 * @return null if no pattern is found
	 */
	public String getCleanPattern() {
		String pattern = getUriPattern();
		if (StringUtils.isEmpty(pattern))
			return null;

		return pattern.replaceAll("\\{[^}]*+}", "*");
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
	 *
	 * @ff.default GET
	 */
	public void setMethod(HttpMethod method) {
		setMethods(method);
	}

	// Can not rename this method to getMethods (or use @Getter), as it will conflict with the types (List vs varargs)
	public List<HttpMethod> getAllMethods() {
		return methods;
	}

	/**
	 * HTTP method(s) to listen to. Inside XML Configurations: for multiple values, use a comma as separator.
	 *
	 * @ff.default GET
	 */
	public void setMethods(HttpMethod... methods) {
		this.methods = List.of(methods);
		if (hasMethod(HttpMethod.OPTIONS)) {
			throw new IllegalArgumentException("method OPTIONS should not be added manually");
		}
	}

	/**
	 * URI pattern to register this listener on, eq. <code>/my-listener/{something}/here</code>
	 *
	 * @ff.mandatory
	 */
	public void setUriPattern(String uriPattern) {
		if (StringUtils.isNotBlank(uriPattern)) {
			if (!uriPattern.startsWith("/"))
				uriPattern = "/" + uriPattern;

			if (uriPattern.endsWith("/"))
				uriPattern = uriPattern.substring(0, uriPattern.length() - 1);
		}
		this.uriPattern = uriPattern;
	}

	/**
	 * The required contentType on requests, if it doesn't match a <code>415</code> status (Unsupported Media Type) is returned.
	 *
	 * @ff.default ANY
	 */
	public void setConsumes(@Nonnull MediaTypes value) {
		this.consumes = value;
	}

	/**
	 * The specified contentType on response. When <code>ANY</code> the response will determine the content-type when it's known and will never calculate it. If no match is found <code>*&#47;*</code> will be used.
	 * When <code>DETECT</code> the framework attempts to detect the MimeType (as well as charset) when not known.
	 *
	 * @ff.default ANY
	 */
	public void setProduces(@Nonnull MediaTypes value) {
		this.produces = value;
	}

	/**
	 * The specified character encoding on the response contentType header. NULL or empty
	 * values will be ignored.
	 *
	 * @ff.default UTF-8
	 */
	public void setCharacterEncoding(String charset) {
		if (StringUtils.isNotBlank(charset)) {
			this.charset = charset;
		}
	}

	/**
	 * Automatically generate and validate etags
	 *
	 * @ff.default <code>false</code>, can be changed by setting the property <code>api.etag.enabled</code>.
	 */
	public void setUpdateEtag(boolean updateEtag) {
		this.updateEtag = updateEtag;
	}

	//TODO add authenticationType

	/**
	 * Enables security for this listener. If you wish to use the application servers authorization roles [AUTHROLE], you need to enable them globally for all ApiListeners with the <code>servlet.ApiListenerServlet.securityRoles=IbisTester,IbisWebService</code> property
	 *
	 * @ff.default <code>NONE</code>
	 */
	public void setAuthenticationMethod(AuthenticationMethods authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

	/**
	 * Only active when AuthenticationMethod=AUTHROLE. Comma separated list of authorization roles which are granted for this service, eq. <code>IbisTester,IbisObserver</code>
	 */
	public void setAuthenticationRoles(String authRoles) {
		this.authenticationRoles = StringUtil.split(authRoles, ",;");
	}

	public List<String> getAuthenticationRoleList() {
		return authenticationRoles;
	}

	/**
	 * Specify the form-part you wish to enter the pipeline
	 *
	 * @ff.default name of the first form-part
	 */
	public void setMultipartBodyName(String multipartBodyName) {
		this.multipartBodyName = multipartBodyName;
	}

	public String getMultipartBodyName() {
		if (StringUtils.isNotEmpty(multipartBodyName)) {
			return multipartBodyName;
		}
		return null;
	}

	/**
	 * Name of the header which contains the Message-Id.
	 */
	@Default(AbstractHttpSender.MESSAGE_ID_HEADER)
	public void setMessageIdHeader(String messageIdHeader) {
		this.messageIdHeader = messageIdHeader;
	}

	/**
	 * Name of the header which contains the Correlation-Id.
	 */
	@Default(AbstractHttpSender.CORRELATION_ID_HEADER)
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

	/** Session key that provides the <code>Content-Disposition</code> header in the response */
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

	/**
	 * Header to extract JWT from
	 *
	 * @ff.default <code>Authorization</code>
	 */
	public void setJwtHeader(String string) {
		this.jwtHeader = string;
	}

	/** Comma separated list of required claims */
	public void setRequiredClaims(String string) {
		this.requiredClaims = string;
	}

	/** Comma separated key value pairs to exactly match with JWT payload. e.g. <code>sub=UnitTest, aud=test</code> */
	public void setExactMatchClaims(String string) {
		this.exactMatchClaims = string;
	}

	/** Comma separated key value pairs to one-of match with JWT payload. e.g. <code>appid=a,appid=b</code> */
	public void setAnyMatchClaims(String string) {
		this.anyMatchClaims = string;
	}

	/** Claim name which specifies the role (maps to <code>IsUserInRolePipe</code>)*/
	public void setRoleClaim(String roleClaim) {
		this.roleClaim = roleClaim;
	}

	/** Claim name which specifies the principal name (maps to <code>GetPrincipalPipe</code>) */
	public void setPrincipalNameClaim(String principalNameClaim) {
		this.principalNameClaim = principalNameClaim;
	}

	/**
	 * Should response be sent as a Multipart or not.
	 * @ff.default false
	 */
	public void setResponseAsMultipart(boolean responseAsMultipart) {
		this.responseAsMultipart = responseAsMultipart;
	}

	/**
	 * If response is sent as Multipart, the type of Multipart entities to be created. Supported types are {@link HttpEntityType#FORMDATA},
	 * {@link HttpEntityType#URLENCODED} and {@link HttpEntityType#MTOM}.
	 */
	public void setResponseEntityType(HttpEntityType responseEntityType) {
		this.responseEntityType = responseEntityType;
	}

	/**
	 * If response is sent as Multipart an optional session key can describe the Multipart contents in XML. See {@link org.frankframework.http.HttpSender#setMultipartXmlSessionKey(String)}
	 * for details on the XML format specification.
	 */
	public void setResponseMultipartXmlSessionKey(String responseMultipartXmlSessionKey) {
		this.responseMultipartXmlSessionKey = responseMultipartXmlSessionKey;
	}

	/**
	 * If response is sent as Multipart, when this option is set the pipeline result message will be prepended as first Multipart Bodypart with
	 * this name.
	 */
	public void setResponseResultBodyPartName(String responseResultBodyPartName) {
		this.responseResultBodyPartName = responseResultBodyPartName;
	}

	public void setResponseMtomContentTransferEncoding(String responseMtomContentTransferEncoding) {
		this.responseMtomContentTransferEncoding = responseMtomContentTransferEncoding;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "@" + Integer.toHexString(hashCode()) +
				" uriPattern[" + getUriPattern() + "]" +
				" produces[" + getProduces() + "]" +
				" consumes[" + getConsumes() + "]" +
				" messageIdHeader[" + getMessageIdHeader() + "]" +
				" updateEtag[" + isUpdateEtag() + "]";
	}

	public static boolean isValidUriPattern(String uriPattern) {
		String uriPatternToValidate = uriPattern.endsWith("/**") ? uriPattern.substring(0, uriPattern.length() - 3) : uriPattern;
		return !VALID_URI_PATTERN_RE.matcher(uriPatternToValidate).find();
	}
}
