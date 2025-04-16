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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nimbusds.jose.util.JSONObjectUtils;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpHeaders;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.http.InputStreamDataSource;
import nl.nn.adapterframework.http.PartMessage;
import nl.nn.adapterframework.http.mime.MultipartUtils;
import nl.nn.adapterframework.http.rest.ApiListener.AuthenticationMethods;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;
import nl.nn.adapterframework.jwt.AuthorizationException;
import nl.nn.adapterframework.jwt.JwtSecurityHandler;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CookieUtil;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.EnumUtils;
import nl.nn.adapterframework.util.HttpUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.MessageUtils;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 *
 * @author Niels Meijer
 *
 */
@IbisInitializer
public class ApiListenerServlet extends HttpServletBase {
	private static final Logger LOG = LogUtil.getLogger(ApiListenerServlet.class);
	private static final long serialVersionUID = 1L;

	public static final String AUTHENTICATION_COOKIE_NAME = "authenticationToken";

	private static final List<String> IGNORE_HEADERS = Arrays.asList("connection", "transfer-encoding", "content-type", "authorization");

	private final int authTTL = AppConstants.getInstance().getInt("api.auth.token-ttl", 60 * 60 * 24 * 7); //Defaults to 7 days
	private final String CorsAllowOrigin = AppConstants.getInstance().getString("api.auth.cors.allowOrigin", "*"); //Defaults to everything
	private final String CorsExposeHeaders = AppConstants.getInstance().getString("api.auth.cors.exposeHeaders", "Allow, ETag, Content-Disposition");
	private static final String UPDATE_ETAG_CONTEXT_KEY = "updateEtag";

	private ApiServiceDispatcher dispatcher = null;
	private IApiCache cache = null;

	@Override
	public void init() throws ServletException {
		if (dispatcher == null) {
			dispatcher = ApiServiceDispatcher.getInstance();
		}
		if (cache == null) {
			cache = ApiCacheManager.getInstance();
		}

		super.init();
	}

	@Override
	public void destroy() {
		if(dispatcher != null) {
			dispatcher.clear();
		}

		super.destroy();
	}

	public void returnJson(HttpServletResponse response, int status, JsonObject json) throws IOException {
		response.setStatus(status);
		Map<String, Boolean> config = new HashMap<>();
		config.put(JsonGenerator.PRETTY_PRINTING, true);
		JsonWriterFactory factory = Json.createWriterFactory(config);
		response.setHeader("Content-Type", "application/json");
		try (JsonWriter jsonWriter = factory.createWriter(response.getOutputStream(), StreamUtil.DEFAULT_CHARSET)) {
			jsonWriter.write(json);
		}
	}

	private static String createEndpointUrlFromRequest(HttpServletRequest request) {
		String requestUrl = request.getRequestURL().toString(); // raw request -> schema+hostname+port/context-path/servlet-path/+request-uri
		requestUrl = HttpUtils.urlDecode(requestUrl);
		String requestPath = request.getPathInfo(); // -> the remaining path, starts with a /. Is automatically decoded by the web container!
		return requestUrl.substring(0, requestUrl.indexOf(requestPath));
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		final String remoteUser = request.getRemoteUser();

		final HttpMethod method;
		try {
			method = EnumUtils.parse(HttpMethod.class, request.getMethod());
		} catch (IllegalArgumentException e) {
			response.setStatus(405);
			LOG.warn("{} method [{}] not allowed", () -> createAbortMessage(remoteUser, 405), request::getMethod);
			return;
		}

		String uri = request.getPathInfo();
		LOG.info("ApiListenerServlet dispatching uri [{}] and method [{}]{}", uri, method, (StringUtils.isNotEmpty(remoteUser) ? " issued by ["+remoteUser+"]" : ""));

		if (uri==null) {
			response.setStatus(400);
			LOG.warn("{} empty uri", () -> createAbortMessage(remoteUser, 400));
			return;
		}
		if(uri.endsWith("/")) {
			uri = uri.substring(0, uri.length()-1);
		}

		try {
			/*
			 * Generate OpenApi specification
			 */
			if(uri.equalsIgnoreCase("/openapi.json")) {
				String endpoint = createEndpointUrlFromRequest(request);
				String specUri = request.getParameter("uri");
				JsonObject jsonSchema = null;
				if(specUri != null) {
					ApiDispatchConfig apiConfig = dispatcher.findConfigForUri(specUri);
					if(apiConfig != null) {
						jsonSchema = dispatcher.generateOpenApiJsonSchema(apiConfig, endpoint);
					}
				} else {
					jsonSchema = dispatcher.generateOpenApiJsonSchema(endpoint);
				}
				if(jsonSchema != null) {
					returnJson(response, 200, jsonSchema);
					return;
				}
				response.sendError(404, "OpenApi specification not found");
				return;
			}

			/*
			 * Generate an OpenApi json file for a set of ApiDispatchConfigs
			 * @Deprecated This is here to support old url's
			 */
			if(uri.endsWith("openapi.json")) {
				String endpoint = createEndpointUrlFromRequest(request);
				uri = uri.substring(0, uri.lastIndexOf("/"));
				ApiDispatchConfig apiConfig = dispatcher.findConfigForUri(uri);
				if(apiConfig != null) {
					JsonObject jsonSchema = dispatcher.generateOpenApiJsonSchema(apiConfig, endpoint);
					returnJson(response, 200, jsonSchema);
					return;
				}
				response.sendError(404, "OpenApi specification not found");
				return;
			}

			handleRequest(request, response, method, uri);
		} finally {
			ThreadContext.clearAll();
		}
	}

	private void handleRequest(HttpServletRequest request, HttpServletResponse response, HttpMethod method, String uri) {
		String remoteUser = request.getRemoteUser();

		/*
		 * Initiate and populate messageContext
		 */
		try (PipeLineSession messageContext = new PipeLineSession()) {
			messageContext.put(PipeLineSession.HTTP_REQUEST_KEY, request);
			messageContext.put(PipeLineSession.HTTP_RESPONSE_KEY, response);
			messageContext.put(PipeLineSession.SERVLET_CONTEXT_KEY, getServletContext());
			messageContext.put("HttpMethod", method);
			messageContext.setSecurityHandler(new HttpSecurityHandler(request));
			try {
				ApiDispatchConfig config = dispatcher.findConfigForUri(uri);
				if(config == null) {
					response.setStatus(404);
					LOG.warn("{} no ApiListener configured for [{}]", ()-> createAbortMessage(remoteUser, 404), ()-> uri);
					return;
				}

				/*
				 * Handle Cross-Origin Resource Sharing
				 * TODO make this work behind loadbalancers/reverse proxies
				 * TODO check if request ip/origin header matches allowOrigin property
				 */
				String origin = request.getHeader("Origin");
				if(method == HttpMethod.OPTIONS || origin != null) {
					response.setHeader("Access-Control-Allow-Origin", CorsAllowOrigin);
					String headers = request.getHeader("Access-Control-Request-Headers");
					if (headers != null)
						response.setHeader("Access-Control-Allow-Headers", headers);
					response.setHeader("Access-Control-Expose-Headers", CorsExposeHeaders);

					StringBuilder methods = new StringBuilder();
					for (HttpMethod mtd : config.getMethods()) {
						methods.append(", ").append(mtd);
					}
					response.setHeader("Access-Control-Allow-Methods", methods.toString());

					//Only cut off OPTIONS (aka preflight) requests
					if(method == HttpMethod.OPTIONS) {
						response.setStatus(200);
						if(LOG.isTraceEnabled()) LOG.trace("Aborting preflight request with status [200], method [{}]", method);
						return;
					}
				}

				/*
				 * Get serviceClient
				 */
				ApiListener listener = config.getApiListener(method);
				if(listener == null) {
					response.setStatus(405);
					LOG.warn("{} method [{}] not allowed", ()-> createAbortMessage(remoteUser, 405), ()-> method);
					return;
				}

				if(LOG.isTraceEnabled()) LOG.trace("ApiListenerServlet calling service [{}]", listener.getName());

				/*
				 * Check authentication
				 */
				ApiPrincipal userPrincipal = null;

				if(listener.getAuthenticationMethod() != AuthenticationMethods.NONE) {
					String authorizationToken = null;
					Cookie authorizationCookie = null;

					switch (listener.getAuthenticationMethod()) {
					case COOKIE:
						authorizationCookie = CookieUtil.getCookie(request, AUTHENTICATION_COOKIE_NAME);
						if(authorizationCookie != null) {
							authorizationToken = authorizationCookie.getValue();
							authorizationCookie.setPath("/");
						}
						break;
					case HEADER:
						authorizationToken = request.getHeader(HttpHeaders.AUTHORIZATION);
						break;
					case AUTHROLE:
						List<String> roles = listener.getAuthenticationRoleList();
						if(roles != null) {
							boolean userIsInRole = roles.stream().anyMatch(request::isUserInRole);
							if(userIsInRole) {
								userPrincipal = new ApiPrincipal();
							}
						}
						break;
					case JWT:
						String authorizationHeader = request.getHeader(listener.getJwtHeader());
						boolean isNonStandardJwtAuthHeader = !HttpHeaders.AUTHORIZATION.equalsIgnoreCase(listener.getJwtHeader());
						if (StringUtils.isNotEmpty(authorizationHeader) && (authorizationHeader.startsWith("Bearer") || isNonStandardJwtAuthHeader)) {
							try {
								String jwtToken = StringUtils.removeStartIgnoreCase(authorizationHeader, "Bearer ");
								Map<String, Object> claimsSet = listener.getJwtValidator().validateJWT(jwtToken);
								messageContext.setSecurityHandler(new JwtSecurityHandler(claimsSet, listener.getRoleClaim(), listener.getPrincipalNameClaim()));
								messageContext.put("ClaimsSet", JSONObjectUtils.toJSONString(claimsSet));
							} catch(Exception e) {
								LOG.warn("unable to validate jwt",e);
								response.sendError(401, e.getMessage());
								return;
							}
						} else {
							response.sendError(401, "JWT is not provided as bearer token");
							return;
						}
						String requiredClaims = listener.getRequiredClaims();
						String exactMatchClaims = listener.getExactMatchClaims();
						String anyMatchClaims = listener.getAnyMatchClaims();
						JwtSecurityHandler handler = (JwtSecurityHandler)messageContext.getSecurityHandler();
						try {
							handler.validateClaims(requiredClaims, exactMatchClaims, anyMatchClaims);
							if(StringUtils.isNotEmpty(listener.getRoleClaim())) {
								List<String> authRoles = listener.getAuthenticationRoleList();
								if(authRoles != null) {
									boolean userIsInRole = authRoles.stream().anyMatch(role -> handler.isUserInRole(role, messageContext));
									if(userIsInRole) {
										userPrincipal = new ApiPrincipal();
									}
								} else {
									userPrincipal = new ApiPrincipal();
								}
							} else {
								userPrincipal = new ApiPrincipal();
							}
						} catch(AuthorizationException e) {
							response.sendError(403, e.getMessage());
							return;
						}

						break;
					default:
						break;
					}

					if(authorizationToken != null && cache.containsKey(authorizationToken))
						userPrincipal = (ApiPrincipal) cache.get(authorizationToken);

					if(userPrincipal == null || !userPrincipal.isLoggedIn()) {
						cache.remove(authorizationToken);
						if(authorizationCookie != null) {
							CookieUtil.addCookie(request, response, authorizationCookie, 0);
						}

						response.setStatus(401);
						LOG.warn("{} no (valid) credentials supplied", ()->createAbortMessage(remoteUser, 401));
						return;
					}

					if(authorizationCookie != null) {
						CookieUtil.addCookie(request, response, authorizationCookie, authTTL);
					}

					if(authorizationToken != null) {
						userPrincipal.updateExpiry();
						userPrincipal.setToken(authorizationToken);
						cache.put(authorizationToken, userPrincipal, authTTL);
						messageContext.put("authorizationToken", authorizationToken);
					}
				}
				// Remove this? it's now available as header value
				messageContext.put("remoteAddr", request.getRemoteAddr());
				if(userPrincipal != null)
					messageContext.put(PipeLineSession.API_PRINCIPAL_KEY, userPrincipal);
				messageContext.put("uri", uri);

				/*
				 * Evaluate preconditions
				 */
				final String acceptHeader = request.getHeader("Accept");
				if(!listener.accepts(acceptHeader)) { // If an Accept header is present, make sure we comply to it!
					LOG.warn("{} client expects Accept [{}] but listener can only provide [{}]", ()->createAbortMessage(request.getRemoteUser(), 406), ()-> acceptHeader, listener::getContentType);
					response.sendError(406, "endpoint cannot provide the supplied MimeType");
					return;
				}

				if(!listener.isConsumable(request.getContentType())) {
					response.setStatus(415);
					LOG.warn("{} did not match consumes [{}] got [{}] instead", ()-> createAbortMessage(remoteUser, 415), listener::getConsumes, request::getContentType);
					return;
				}

				String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
				LOG.debug("Evaluating preconditions for listener[{}] etagKey[{}]", listener.getName(), etagCacheKey);
				if(cache.containsKey(etagCacheKey)) {
					String cachedEtag = (String) cache.get(etagCacheKey);
					LOG.debug("found etag value[{}] for key[{}]", cachedEtag, etagCacheKey);

					if(method == HttpMethod.GET) {
						String ifNoneMatch = request.getHeader("If-None-Match");
						if(ifNoneMatch != null && ifNoneMatch.equals(cachedEtag)) {
							response.setStatus(304);
							if (LOG.isDebugEnabled()) LOG.debug("{} matched if-none-match [{}]", ()->createAbortMessage(remoteUser, 304), ()->ifNoneMatch);
							return;
						}
					}
					else {
						String ifMatch = request.getHeader("If-Match");
						if(ifMatch != null && !ifMatch.equals(cachedEtag)) {
							response.setStatus(412);
							LOG.warn("{} matched if-match [{}] method [{}]", ()->createAbortMessage(remoteUser, 412), ()->ifMatch, ()->method);
							return;
						}
					}
				}
				messageContext.put(UPDATE_ETAG_CONTEXT_KEY, listener.isUpdateEtag());

				/*
				 * Check authorization
				 */
				//TODO: authentication implementation

				/*
				 * Map uriIdentifiers into messageContext
				 */
				String[] patternSegments = listener.getUriPattern().split("/");
				String[] uriSegments = uri.split("/");
				int uriIdentifier = 0;
				for (int i = 0; i < patternSegments.length; i++) {
					final String segment = patternSegments[i];
					final String name;

					if("*".equals(segment)) {
						name = "uriIdentifier_"+uriIdentifier;
					}
					else if(segment.startsWith("{") && segment.endsWith("}")) {
						name = segment.substring(1, segment.length()-1);
					}
					else {
						name = null;
					}

					if(name != null) {
						uriIdentifier++;
						if(LOG.isTraceEnabled()) LOG.trace("setting uriSegment [{}] to [{}]", name, uriSegments[i]);
						messageContext.put(name, uriSegments[i]);
					}
				}

				/*
				 * Map queryParameters into messageContext
				 */
				messageContext.putAll(extractRequestParams(request));

				/*
				 * Map headers into messageContext
				 */
				if(StringUtils.isNotEmpty(listener.getHeaderParams())) {
					messageContext.put("headers", extractHeaderParamsAsXml(request, listener));
				}

				/*
				 * Process the request through the pipeline.
				 * If applicable, map multipart parts into messageContext
				 */
				Message body = Message.nullMessage();
				//TODO fix HttpSender#handleMultipartResponse(..)
				if(MultipartUtils.isMultipart(request)) {
					final String multipartBodyName = listener.getMultipartBodyName();
					try {
						final InputStreamDataSource dataSource = new InputStreamDataSource(request.getContentType(), request.getInputStream()); //the entire InputStream will be read here!
						final MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
						final XmlBuilder attachments = new XmlBuilder("parts");

						for (int i = 0; i < mimeMultipart.getCount(); i++) {
							final BodyPart bodyPart = mimeMultipart.getBodyPart(i);
							final String fieldName = MultipartUtils.getFieldName(bodyPart);
							if((i == 0 && multipartBodyName == null) || (fieldName != null && fieldName.equalsIgnoreCase(multipartBodyName))) {
								body = new PartMessage(bodyPart, MessageUtils.getContext(request));
							}

							final XmlBuilder attachment = new XmlBuilder("part");
							attachment.addAttribute("name", fieldName);
							PartMessage message = new PartMessage(bodyPart);
							if (!MultipartUtils.isBinary(bodyPart)) {
								// Process regular form field (input type="text|radio|checkbox|etc", select, etc).
								LOG.trace("setting multipart formField [{}] to [{}]", fieldName, message);
								messageContext.put(fieldName, message.asString());
								attachment.addAttribute("type", "text");
								attachment.addAttribute("value", message.asString());
							} else {
								// Process form file field (input type="file").
								final String fieldNameName = fieldName + "Name";
								final String fileName = MultipartUtils.getFileName(bodyPart);
								LOG.trace("setting multipart formFile [{}] to [{}]", fieldNameName, fileName);
								messageContext.put(fieldNameName, fileName);
								LOG.trace("setting parameter [{}] to input stream of file [{}]", fieldName, fileName);
								messageContext.put(fieldName, message);

								attachment.addAttribute("type", "file");
								attachment.addAttribute("filename", fileName);
								attachment.addAttribute("size", message.size());
								attachment.addAttribute("sessionKey", fieldName);
								attachment.addAttribute("mimeType", extractMimeType(bodyPart.getContentType()));
							}
							attachments.addSubElement(attachment);
						}
						messageContext.put("multipartAttachments", attachments.toXML());
					} catch(MessagingException e) {
						response.sendError(400, "Could not read mime multipart request");
						LOG.warn("{} Could not read mime multipart request: {}", () -> createAbortMessage(remoteUser, 400), e::getMessage);
						return;
					}
				} else {
					body = MessageUtils.parseContentAsMessage(request);
				}

				/*
				 * Compile Allow header
				 */
				messageContext.put("allowedMethods", buildAllowedMethodsHeader(config.getMethods()));

				final String messageId = getHeaderOrDefault(request, listener.getMessageIdHeader(), null);
				final String correlationId = getHeaderOrDefault(request, listener.getCorrelationIdHeader(), messageId);
				PipeLineSession.updateListenerParameters(messageContext, messageId, correlationId, null, null); //We're only using this method to keep setting mid/cid uniform

				/*
				 * Do the actual request processing by the ApiListener
				 */
				Message result = listener.processRequest(body, messageContext);

				/*
				 * Calculate an eTag over the processed result and store in cache
				 */
				if (Boolean.TRUE.equals(messageContext.getBoolean(UPDATE_ETAG_CONTEXT_KEY))) {
					LOG.debug("calculating etags over processed result");
					String cleanPattern = listener.getCleanPattern();
					if(!Message.isEmpty(result) && method == HttpMethod.GET && cleanPattern != null) { //If the data has changed, generate a new eTag
						String eTag = MessageUtils.generateMD5Hash(result);
						if(eTag != null) {
							LOG.debug("adding/overwriting etag with key[{}] value[{}]", etagCacheKey, eTag);
							cache.put(etagCacheKey, eTag);
							response.addHeader("etag", eTag);
						} else {
							LOG.debug("skipping etag with key[{}] computed value is null", etagCacheKey);
						}
					}
					else {
						LOG.debug("removing etag with key[{}]", etagCacheKey);
						cache.remove(etagCacheKey);

						// Not only remove the eTag for the selected resources but also the collection
						String key = ApiCacheManager.getParentCacheKey(listener, uri);
						if(key != null) {
							LOG.debug("removing parent etag with key[{}]", key);
							cache.remove(key);
						}
					}
				}

				/*
				 * If a Last Modified value is present, set the 'Last-Modified' header.
				 */
				long lastModDate = Instant.now().toEpochMilli();
				if(!Message.isEmpty(result)) {
					String lastModified = (String) result.getContext().get(MessageContext.METADATA_MODIFICATIONTIME);
					if(StringUtils.isNotEmpty(lastModified)) {
						Date date = DateUtils.parseToDate(lastModified, DateUtils.FORMAT_FULL_GENERIC);
						if(date != null) {
							lastModDate = date.getTime();
						}
					}
				}
				response.setDateHeader("Last-Modified", lastModDate);
				// If no eTag header is present, disable browser caching. Else force browser to revalidate the request.
				StringBuilder cacheControl = new StringBuilder();
				if(!response.containsHeader("etag")) {
					cacheControl.append("no-store, no-cache, ");
					response.setHeader("Pragma", "no-cache");
					LOG.trace("disabling cache for uri [{}]", request::getRequestURI);
				}
				cacheControl.append("must-revalidate, max-age=0, post-check=0, pre-check=0");
				response.setHeader("Cache-Control", cacheControl.toString());

				/*
				 * Add headers
				 */
				response.addHeader("Allow", (String) messageContext.get("allowedMethods"));

				if (!Message.isEmpty(result)) {
					MimeType contentType = determineContentType(messageContext, listener, result);
					response.setContentType(contentType.toString());
				}

				if(StringUtils.isNotEmpty(listener.getContentDispositionHeaderSessionKey())) {
					String contentDisposition = messageContext.getString(listener.getContentDispositionHeaderSessionKey());
					if(StringUtils.isNotEmpty(contentDisposition)) {
						LOG.debug("Setting Content-Disposition header to [{}]", contentDisposition);
						response.setHeader("Content-Disposition", contentDisposition);
					}
				}

				/*
				 * Check if an exitcode has been defined or if a status-code has been added to the messageContext.
				 */
				int statusCode = messageContext.get(PipeLineSession.EXIT_CODE_CONTEXT_KEY, 0);
				if(statusCode > 0) {
					response.setStatus(statusCode);
				}

				/*
				 * Finalize the pipeline and write the result to the response
				 */
				final boolean outputWritten = writeToResponseStream(response, result);
				if (!outputWritten) {
					LOG.debug("No output written, set content-type header to null");
					response.resetBuffer();
					response.setContentType(null);
				}

				LOG.trace("ApiListenerServlet finished with statusCode [{}] result [{}]", statusCode, result);
			}
			catch (Exception e) {
				LOG.warn("ApiListenerServlet caught exception, will rethrow as ServletException", e);
				try {
					response.reset();
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				}
				catch (IOException | IllegalStateException ex) {
					LOG.warn("an error occurred while trying to handle exception [{}]", e.getMessage(), ex);
					//We're only informing the end user(s), no need to catch this error...
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
		}
	}

	@Nonnull
	private Map<String, Object> extractRequestParams(HttpServletRequest request) {
		Map<String, Object> params = new HashMap<>();
		Enumeration<String> paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String paramName = paramNames.nextElement();
			String[] paramList = request.getParameterValues(paramName);
			if(paramList.length > 1) { // contains multiple items
				List<String> valueList = Arrays.asList(paramList);
				if(LOG.isTraceEnabled()) LOG.trace("setting queryParameter [{}] to {}", paramName, valueList);
				params.put(paramName, valueList);
			}
			else {
				String paramValue = request.getParameter(paramName);
				if(LOG.isTraceEnabled()) LOG.trace("setting queryParameter [{}] to [{}]", paramName, paramValue);
				params.put(paramName, paramValue);
			}
		}
		return params;
	}

	private String extractHeaderParamsAsXml(HttpServletRequest request, ApiListener listener) {
		XmlBuilder headersXml = new XmlBuilder("headers");
		String[] params = listener.getHeaderParams().split(",");
		for (String headerParam : params) {
			if(IGNORE_HEADERS.contains(headerParam)) {
				continue;
			}
			String headerValue = request.getHeader(headerParam);
			try {
				XmlBuilder headerXml = new XmlBuilder("header");
				headerXml.addAttribute("name", headerParam);
				headerXml.setValue(headerValue);
				headersXml.addSubElement(headerXml);
			}
			catch (Exception e) {
				LOG.info("unable to convert header to xml name[{}] value[{}], exception message: {}", headerParam, headerValue, e.getMessage());
			}
		}
		return headersXml.toXML();
	}

	private static @Nonnull MimeType determineContentType(PipeLineSession messageContext, ApiListener listener, Message result) throws IOException {
		if(listener.getProduces() == MediaTypes.ANY) {
			Message parsedContentType = messageContext.getMessage("contentType");
			if(!Message.isEmpty(parsedContentType)) {
				try {
					return MimeType.valueOf(parsedContentType.asString());
				} catch (InvalidMimeTypeException imte) {
					LOG.warn("unable to parse mimetype from SessionKey [contentType] value [{}]", parsedContentType, imte);
				}
			}
			MimeType providedContentType = MessageUtils.getMimeType(result); // MimeType might be known
			if(providedContentType != null) {
				return providedContentType;
			}
		} else if(listener.getProduces() == MediaTypes.DETECT) {
			MimeType computedContentType = MessageUtils.computeMimeType(result); // Calculate MimeType
			if(computedContentType != null) {
				return computedContentType;
			}
		}
		return listener.getContentType();
	}

	/**
	 * Write the result to the response, if any data is available. If no data is
	 * available, then the output-stream or output-writer of the response will not
	 * be accessed and the method returns false. If data is available, the method
	 * returns true and the data will be
	 * written to the output-stream if the message is binary or to the output-writer
	 * otherwise.
	 *
	 * @param response {@link HttpServletResponse} to which data should be written. If
	 *                                            no data is available, the output-stream or
	 *                                            output-writer will not be accessed.
	 * @param result {@link Message} whose data will be written to the response, if any is available.
	 * @return {@code true} if data was written, {@code false} if not.
	 * @throws IOException Thrown if reading or writing to / from any of the streams throws  an IOException.
	 */
	private static boolean writeToResponseStream(HttpServletResponse response, Message result) throws IOException {
		if (!Message.hasDataAvailable(result)) {
			return false;
		}
		if (result.isBinary()) {
			try (InputStream in = result.asInputStream()) {
				StreamUtil.copyStream(in, response.getOutputStream(), 4096);
			}
		} else {
			try (Reader reader = result.asReader()) {
				StreamUtil.copyReaderToWriter(reader, response.getWriter(), 4096);
			}
		}
		return true;
	}

	private String getHeaderOrDefault(HttpServletRequest request, String headerName, String defaultValue) {
		if (StringUtils.isBlank(headerName)) {
			return defaultValue;
		}
		final String headerValue = request.getHeader(headerName);
		return StringUtils.isNotBlank(headerValue) ? headerValue : defaultValue;
	}

	private String extractMimeType(String contentType) {
		final int semicolon = contentType.indexOf(";");
		if(semicolon >= 0) {
			return contentType.substring(0, semicolon);
		} else {
			return contentType;
		}
	}

	private String buildAllowedMethodsHeader(Set<HttpMethod> methods) {
		StringBuilder methodsBuilder = new StringBuilder();
		methodsBuilder.append("OPTIONS");
		for (HttpMethod mtd : methods) {
			if (mtd != HttpMethod.OPTIONS) {
				methodsBuilder
					.append(", ")
					.append(mtd);
			}
		}
		return methodsBuilder.toString();
	}

	@Override
	public String getUrlMapping() {
		return "/api/*";
	}

	private String createAbortMessage(String remoteUser, int statusCode) {
		StringBuilder message = new StringBuilder("Aborting request ");
		if(StringUtils.isNotEmpty(remoteUser)) {
			message
				.append("issued by [")
				.append(remoteUser)
				.append("] ");
		}
		message
			.append("with status code [")
			.append(statusCode)
			.append("], ");
		return message.toString();
	}
}
