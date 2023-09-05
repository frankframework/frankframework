/*
   Copyright 2017-2023 WeAreFrank!

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
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

import com.nimbusds.jose.util.JSONObjectUtils;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;
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

	private int authTTL = AppConstants.getInstance().getInt("api.auth.token-ttl", 60 * 60 * 24 * 7); //Defaults to 7 days
	private String CorsAllowOrigin = AppConstants.getInstance().getString("api.auth.cors.allowOrigin", "*"); //Defaults to everything
	private String CorsExposeHeaders = AppConstants.getInstance().getString("api.auth.cors.exposeHeaders", "Allow, ETag, Content-Disposition");
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
		try (JsonWriter jsonWriter = factory.createWriter(response.getOutputStream(), StreamUtil.DEFAULT_CHARSET)) {
			jsonWriter.write(json);
		}
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String remoteUser = request.getRemoteUser();

		HttpMethod method;
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
				String specUri = request.getParameter("uri");
				JsonObject jsonSchema = null;
				if(specUri != null) {
					ApiDispatchConfig apiConfig = dispatcher.findConfigForUri(specUri);
					if(apiConfig != null) {
						jsonSchema = dispatcher.generateOpenApiJsonSchema(apiConfig, request);
					}
				} else {
					jsonSchema = dispatcher.generateOpenApiJsonSchema(request);
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
				uri = uri.substring(0, uri.lastIndexOf("/"));
				ApiDispatchConfig apiConfig = dispatcher.findConfigForUri(uri);
				if(apiConfig != null) {
					JsonObject jsonSchema = dispatcher.generateOpenApiJsonSchema(apiConfig, request);
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
						authorizationToken = request.getHeader("Authorization");
						break;
					case AUTHROLE:
						List<String> roles = listener.getAuthenticationRoleList();
						if(roles != null) {
							for (String role : roles) {
								if(request.isUserInRole(role)) {
									userPrincipal = new ApiPrincipal(); //Create a dummy user
									break;
								}
							}
						}
						break;
					case JWT:
						String authorizationHeader = request.getHeader(listener.getJwtHeader());
						if(StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.contains("Bearer")) {
							try {
								Map<String, Object> claimsSet = listener.getJwtValidator().validateJWT(authorizationHeader.substring(7));
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
						JwtSecurityHandler handler = (JwtSecurityHandler)messageContext.getSecurityHandler();
						try {
							handler.validateClaims(requiredClaims, exactMatchClaims);
							if(StringUtils.isNotEmpty(listener.getRoleClaim())) {
								List<String> authRoles = listener.getAuthenticationRoleList();
								if(authRoles != null) {
									for (String role : authRoles) {
										if(handler.isUserInRole(role, messageContext)) {
											userPrincipal = new ApiPrincipal();
											break;
										}
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
				if(StringUtils.isNotEmpty(acceptHeader) && !listener.accepts(acceptHeader)) { // If an Accept header is present, make sure we comply to it!
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
					String segment = patternSegments[i];
					String name = null;

					if("*".equals(segment)) {
						name = "uriIdentifier_"+uriIdentifier;
					}
					else if(segment.startsWith("{") && segment.endsWith("}")) {
						name = segment.substring(1, segment.length()-1);
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
				Enumeration<String> paramnames = request.getParameterNames();
				while (paramnames.hasMoreElements()) {
					String paramname = paramnames.nextElement();
					String[] paramList = request.getParameterValues(paramname);
					if(paramList.length > 1) { // contains multiple items
						List<String> valueList = Arrays.asList(paramList);
						LOG.trace("setting queryParameter [{}] to [{}]", paramname, valueList);
						messageContext.put(paramname, valueList);
					}
					else {
						String paramvalue = request.getParameter(paramname);
						LOG.trace("setting queryParameter [{}] to [{}]", paramname, paramvalue);
						messageContext.put(paramname, paramvalue);
					}
				}

				/*
				 * Map headers into messageContext
				 */
				if(StringUtils.isNotEmpty(listener.getHeaderParams())) {
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
						catch (Throwable t) {
							LOG.info("unable to convert header to xml name[{}] value[{}]", headerParam, headerValue);
						}
					}
					messageContext.put("headers", headersXml.toXML());
				}

				/*
				 * Process the request through the pipeline.
				 * If applicable, map multipart parts into messageContext
				 */
				Message body = null;
				//TODO fix HttpSender#handleMultipartResponse(..)
				if(MultipartUtils.isMultipart(request)) {
					String multipartBodyName = listener.getMultipartBodyName();
					try {
						InputStreamDataSource dataSource = new InputStreamDataSource(request.getContentType(), request.getInputStream()); //the entire InputStream will be read here!
						MimeMultipart mimeMultipart = new MimeMultipart(dataSource);
						XmlBuilder attachments = new XmlBuilder("parts");

						for (int i = 0; i < mimeMultipart.getCount(); i++) {
							BodyPart bodyPart = mimeMultipart.getBodyPart(i);
							String fieldName = MultipartUtils.getFieldName(bodyPart);
							if((i == 0 && multipartBodyName == null) || (fieldName != null && fieldName.equalsIgnoreCase(multipartBodyName))) {
								body = new PartMessage(bodyPart, MessageUtils.getContext(request));
							}

							XmlBuilder attachment = new XmlBuilder("part");
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
								String contentType = bodyPart.getContentType();
								String mimeType = contentType;
								int semicolon = contentType.indexOf(";");
								if(semicolon >= 0) {
									mimeType = contentType.substring(0, semicolon);
								}

								attachment.addAttribute("mimeType", mimeType);
							}
							attachments.addSubElement(attachment);
						}
						messageContext.put("multipartAttachments", attachments.toXML());
					} catch(MessagingException e) {
						response.sendError(400, "Could not read mime multipart response");
						LOG.warn("{} Could not read mime multipart response", () -> createAbortMessage(remoteUser, 400));
						return;
					}
				} else {
					body = MessageUtils.parseContentAsMessage(request);
				}

				/*
				 * Compile Allow header
				 */
				StringBuilder methods = new StringBuilder();
				methods.append("OPTIONS, ");
				for (HttpMethod mtd : config.getMethods()) {
					methods.append(mtd).append(", ");
				}
				messageContext.put("allowedMethods", methods.substring(0, methods.length()-2));

				String messageId = null;
				if(StringUtils.isNotEmpty(listener.getMessageIdHeader())) {
					String messageIdHeader = request.getHeader(listener.getMessageIdHeader());
					if(StringUtils.isNotEmpty(messageIdHeader)) {
						messageId = messageIdHeader;
					}
				}
				PipeLineSession.setListenerParameters(messageContext, messageId, null, null, null); //We're only using this method to keep setting id/cid/tcid uniform
				Message result = listener.processRequest(null, body, messageContext);

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
					String contentDisposition = messageContext.getMessage(listener.getContentDispositionHeaderSessionKey()).asString();
					if(StringUtils.isNotEmpty(contentDisposition)) {
						LOG.debug("Setting Content-Disposition header to [{}]", contentDisposition);
						response.setHeader("Content-Disposition", contentDisposition);
					}
				}

				/*
				 * Check if an exitcode has been defined or if a statuscode has been added to the messageContext.
				 */
				int statusCode = messageContext.get("exitcode", 0);
				if(statusCode > 0) {
					response.setStatus(statusCode);
				}

				/*
				 * Finalize the pipeline and write the result to the response
				 */
				if(!Message.isEmpty(result)) {
					if(result.isBinary()) {
						StreamUtil.copyStream(result.asInputStream(), response.getOutputStream(), 4096);
					} else {
						StreamUtil.copyReaderToWriter(result.asReader(), response.getWriter(), 4096, false, false);
					}
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

	@Override
	public String getUrlMapping() {
		return "/api/*";
	}

	private String createAbortMessage(String remoteUser, int statusCode) {
		StringBuilder message = new StringBuilder("");
		message.append("Aborting request ");
		if(StringUtils.isNotEmpty(remoteUser)) {
			message.append("issued by ["+remoteUser+"] ");
		}
		message.append("with status code ["+statusCode+"], ");
		return message.toString();
	}
}
