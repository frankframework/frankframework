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
package org.frankframework.http.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.util.MimeType;

import com.nimbusds.jose.proc.SecurityContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.DestinationType;
import org.frankframework.core.DestinationType.Type;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.PipeLineSession;
import org.frankframework.doc.Default;
import org.frankframework.http.AbstractHttpSender;
import org.frankframework.http.HttpEntityType;
import org.frankframework.http.PushingListenerAdapter;
import org.frankframework.http.mime.HttpEntityFactory;
import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.jwt.JwtValidator;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.lifecycle.ServletManager;
import org.frankframework.lifecycle.servlets.ServletConfiguration;
import org.frankframework.receivers.Receiver;
import org.frankframework.receivers.ReceiverAware;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StringUtil;

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
 * @see <a href="https://swagger.io/specification/">OpenAPI Reference Specification</a>
 * 
 * @ff.tip The OPTIONS verb will automatically be handled by the framework.
 *
 * @author Niels Meijer
 */
@DestinationType(Type.HTTP)
public class ApiListener extends PushingListenerAdapter implements HasPhysicalDestination, ReceiverAware<Message> {

	private static final Pattern VALID_URI_PATTERN_RE = Pattern.compile("([^/]\\*|\\*[^/\\n])");
	private static final Pattern URI_PATTERN_VARIABLES_RE = Pattern.compile("/\\{(.+?)}(?=/|$)");

	/**
	 * These are names that are never allowed as HTTP parameters, because the Frank!Framework sets these names as session variables.
	 */
	public static final Set<String> RESERVED_NAMES = Set.of(PipeLineSession.ORIGINAL_MESSAGE_KEY, PipeLineSession.API_PRINCIPAL_KEY, PipeLineSession.HTTP_METHOD_KEY, PipeLineSession.HTTP_REQUEST_KEY, PipeLineSession.HTTP_RESPONSE_KEY, PipeLineSession.SECURITY_HANDLER_KEY, "ClaimsSet", "allowedMethods", "headers", ApiListenerServlet.UPDATE_ETAG_CONTEXT_KEY, "uri", "remoteAddr", ApiListenerServlet.AUTHENTICATION_COOKIE_NAME, MultipartUtils.MULTIPART_ATTACHMENTS_SESSION_KEY);

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
	private @Getter @Nonnull Set<String> allowedParameterSet = Set.of();
	private @Getter Boolean allowAllParams = null;

	// for jwt validation
	private @Getter String requiredIssuer = null;
	private @Getter String jwksUrl = null;
	private @Getter String jwtHeader = HttpHeaders.AUTHORIZATION;
	private @Getter String requiredClaims = null;
	private @Getter String exactMatchClaims = null;
	private @Getter String anyMatchClaims = null;
	private @Getter String roleClaim;

	private @Getter String principalNameClaim = "sub";
	private @Getter String physicalDestinationName = null;

	private @Getter JwtValidator<SecurityContext> jwtValidator;
	private @Setter ServletManager servletManager;

	private @Getter HttpEntityType responseType;
	private @Getter String responseMultipartXmlSessionKey;
	private @Getter String responseFirstBodyPartName;
	private @Getter String responseMtomContentTransferEncoding;

	private @Getter HttpEntityFactory responseEntityBuilder;

	public enum AuthenticationMethods {
		NONE, COOKIE, HEADER, AUTHROLE, JWT;
	}

	/**
	 * initialize listener and register <code>this</code> to the JNDI
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (hasMethod(HttpMethod.OPTIONS)) {
			throw new ConfigurationException("method OPTIONS should not be added manually as it's automatically handled by the application");
		}

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

		if (responseType == null) {
			responseType = HttpEntityType.BINARY;
		}
		responseEntityBuilder = HttpEntityFactory.Builder.create()
				.entityType(responseType)
				.multipartXmlSessionKey(responseMultipartXmlSessionKey)
				.firstBodyPartName(responseFirstBodyPartName)
				.mtomContentTransferEncoding(responseMtomContentTransferEncoding)
				.build();

		if (responseType != HttpEntityType.MTOM && responseType != HttpEntityType.FORMDATA) {
			if (StringUtils.isNotBlank(responseMultipartXmlSessionKey)) {
				ConfigurationWarnings.add(this, log, "[responseMultipartXmlSessionKey] should only be set when [responseType] is [MTOM] or [FORMDATA]");
			}
			if (StringUtils.isNotBlank(responseFirstBodyPartName)) {
				ConfigurationWarnings.add(this, log, "[responseResultBodyPartName] should only be set when [responseType] is [MTOM] or [FORMDATA]");
			}
		}
		if (responseType != HttpEntityType.MTOM && StringUtils.isNotBlank(responseMtomContentTransferEncoding)) {
			ConfigurationWarnings.add(this, log, "[responseMtomContentTransferEncoding] should only be set when [responseType] is [MTOM]");
		}

		// Check that none of configured parameters or path-variables matches any of the reserved names.
		if (allowedParameterSet.isEmpty() && allowAllParams == null) {
			ConfigurationWarnings.add(this, log, "SECURITY RISK: All path parameters and query parameters will be copied into the session. Specify [allowedParameters] for your pipeline, or explicitly set [allowAllParams] to 'true'.", SuppressKeys.UNSAFE_ATTRIBUTE_SUPPRESS_KEY);
			allowAllParams = true;
		}
		Set<String> paramsFromBlacklist = new HashSet<>(allowedParameterSet);
		paramsFromBlacklist.retainAll(RESERVED_NAMES);
		if (!paramsFromBlacklist.isEmpty()) {
			ConfigurationWarnings.add(this, log, "[allowedParameters] contains reserved names that are not allowed as HTTP parameter names, these are removed: [" + paramsFromBlacklist + "]", SuppressKeys.UNSAFE_ATTRIBUTE_SUPPRESS_KEY);
			allowedParameterSet.removeAll(paramsFromBlacklist);
		}
		if (StringUtils.isNotEmpty(getUriPattern())) {
			Set<String> forbiddenPathVariables = new HashSet<>();
			Matcher variableSegmentMatcher =  URI_PATTERN_VARIABLES_RE.matcher(getUriPattern());
			while (variableSegmentMatcher.find()) {
				String pathVariable = variableSegmentMatcher.group(1);
				if (RESERVED_NAMES.contains(pathVariable)) {
					forbiddenPathVariables.add(pathVariable);
				}
			}
			if (!forbiddenPathVariables.isEmpty()) {
				throw new ConfigurationException("URI Pattern contains reserved names as path variables, these need to be renamed: [" + forbiddenPathVariables + "]");
			}
		}
		if (getMultipartBodyName() != null && RESERVED_NAMES.contains(getMultipartBodyName())) {
			throw new ConfigurationException("[multipartBodyName] is a reserved name that cannot be used for any kind of request parameter, set to [" + getMultipartBodyName() + "]");
		}
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

	public boolean isParameterAllowed(@Nonnull String parameterName) {
		if (allowedParameterSet.isEmpty() && isAllowAllParams()) {
			return !RESERVED_NAMES.contains(parameterName);
		}
		return allowedParameterSet.contains(parameterName);
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
	}

	/**
	 * URI pattern to register this listener on, eq. <code>/my-listener/{something}/here</code>.
	 * <br/>
	 * Pattern variables like {@code {something}} in this example are added to the PipeLineSession with
	 * their actual value in the request URI.
	 * <br/>
	 * Pattern variables are not allowed to have the same name as any of the {@link #RESERVED_NAMES}.
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
	 * Specify the form-part you wish to enter the pipeline.
	 * <br/>
	 * The {@code multipartBodyName} or the names of any other multipart
	 * fields may not be one of the {@link #RESERVED_NAMES}.
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

	/**
	 * Whitelist of request parameters (Query and POST Parameters) that are allowed to be
	 * copied into the session.
	 * <br/>
	 * Entered as a comma-separated value.
	 * <br/>
	 * If the list contains any names that are in {@link #RESERVED_NAMES}, these will be removed from the list.
	 * <br/>
	 * If left empty, then all HTTP parameters are copied into the session, which can pose
	 * a security risk and is therefore discouraged. The risk is that parameters could be sent,
	 * that overwrite system session variables.
	 * <br/>
	 * This only works as a backwards-compatibility feature and can be switched off setting {@link #setAllowAllParams(boolean)} to {@code false}.
	 *
	 * @param paramWhitelist Comma-separated list of allowed HTTP parameters.
	 */
	public void setAllowedParameters(@Nullable String paramWhitelist) {
		this.allowedParameterSet = StringUtil.splitToStream(paramWhitelist).collect(Collectors.toSet());
	}

	/**
	 * For backwards compatibility with configurations that have not yet been updated, by
	 * default all parameters are allowed until removal of this flag.
	 * Copying all POST and query parameters to the session is considered a security risk,
	 * so this should not be left enabled.
	 * <br/>
	 * Even so, names listed in {@link #RESERVED_NAMES} will never be copied from the HTTP parameters to the session.
	 * <br/>
	 * When setting {@link #setAllowedParameters(String)}, this value is ignored. This value is only
	 * used when the allowed parameter list has not been set, or set empty.
	 * <br/>
	 * For backwards compatibility, this is {@code true} by default.
	 *
	 * @ff.default true
	 */
	public void setAllowAllParams(boolean allowAllParams) {
		this.allowAllParams = allowAllParams;
	}

	public boolean isAllowAllParams() {
		if (this.allowAllParams == null) return true;
		return this.allowAllParams;
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
	 * Optional configuration setting to have more control over how to send the response.
	 * Set this to return data as Multipart formdata or MTOM.
	 * See {@link HttpEntityType} for all supported values and how to use them.
	 *
	 * @ff.note NB: For the ApiListener, there is no difference between {@link HttpEntityType#BINARY} and {@link HttpEntityType#RAW} because parameters are not supported.
	 *
	 * @ff.default {@link HttpEntityType#BINARY}.
	 */
	public void setResponseType(HttpEntityType responseType) {
		this.responseType = responseType;
	}

	/**
	 * If response is sent as Multipart ({@link HttpEntityType#FORMDATA} or {@link HttpEntityType#MTOM}) an optional session key can describe the Multipart contents in XML. See {@link org.frankframework.http.HttpSender#setMultipartXmlSessionKey(String)}
	 * for details on the XML format specification.
	 */
	public void setResponseMultipartXmlSessionKey(String responseMultipartXmlSessionKey) {
		this.responseMultipartXmlSessionKey = responseMultipartXmlSessionKey;
	}

	/**
	 * If response is sent as Multipart ({@link HttpEntityType#FORMDATA} or {@link HttpEntityType#MTOM}), when this option is set the pipeline result message will be prepended as first Multipart Bodypart with
	 * this name.
	 */
	public void setResponseFirstBodyPartName(String responseFirstBodyPartName) {
		this.responseFirstBodyPartName = responseFirstBodyPartName;
	}

	/**
	 * If the response is sent as {@link HttpEntityType#MTOM}, optionally specify the transfer-encoding of the first part.
	 */
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
