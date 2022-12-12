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

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
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
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.CookieUtil;
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
	protected Logger log = LogUtil.getLogger(this);
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
		response.setHeader("Content-Type", "application/json");
		try (JsonWriter jsonWriter = factory.createWriter(response.getOutputStream(), StreamUtil.DEFAULT_CHARSET)) {
			jsonWriter.write(json);
		}
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String uri = request.getPathInfo();
		String remoteUser = request.getRemoteUser();

		HttpMethod method;
		try {
			method = EnumUtils.parse(HttpMethod.class, request.getMethod());
		} catch (IllegalArgumentException e) {
			response.setStatus(405);
			log.warn(createAbortMessage(remoteUser, 405) + "method ["+request.getMethod()+"] not allowed");
			return;
		}

		if(log.isInfoEnabled()) {
			String infoMessage = "ApiListenerServlet dispatching uri ["+uri+"] and method ["+method+"]" + (StringUtils.isNotEmpty(remoteUser) ? " issued by ["+remoteUser+"]" : "");
			log.info(infoMessage);
		}

		if (uri==null) {
			response.setStatus(400);
			log.warn(createAbortMessage(remoteUser, 400) + "empty uri");
			return;
		}
		if(uri.endsWith("/")) {
			uri = uri.substring(0, uri.length()-1);
		}

		/**
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

		/**
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
	}

	private void handleRequest(HttpServletRequest request, HttpServletResponse response, HttpMethod method, String uri) {
		String remoteUser = request.getRemoteUser();

		/**
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
					log.warn(createAbortMessage(remoteUser, 404) + "no ApiListener configured for ["+uri+"]");
					return;
				}

				/**
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
						if(log.isTraceEnabled()) log.trace("Aborting preflight request with status [200], method ["+method+"]");
						return;
					}
				}

				/**
				 * Get serviceClient
				 */
				ApiListener listener = config.getApiListener(method);
				if(listener == null) {
					response.setStatus(405);
					log.warn(createAbortMessage(remoteUser, 405) + "method ["+method+"] not allowed");
					return;
				}

				if(log.isTraceEnabled()) log.trace("ApiListenerServlet calling service ["+listener.getName()+"]");

				/**
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
						String authorizationHeader = request.getHeader("Authorization");
						if(StringUtils.isNotEmpty(authorizationHeader) && authorizationHeader.contains("Bearer")) {
							try {
								Map<String, Object> claimsSet = listener.getJwtValidator().validateJWT(authorizationHeader.substring(7));
								messageContext.setSecurityHandler(new JwtSecurityHandler(claimsSet, listener.getRoleClaim()));
								messageContext.put("ClaimsSet", JSONObjectUtils.toJSONString(claimsSet));
							} catch(Exception e) {
								log.warn("unable to validate jwt",e);
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
						log.warn(createAbortMessage(remoteUser, 401) + "no (valid) credentials supplied");
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
				//Remove this? it's now available as header value
				messageContext.put("remoteAddr", request.getRemoteAddr());
				if(userPrincipal != null)
					messageContext.put(PipeLineSession.API_PRINCIPAL_KEY, userPrincipal);
				messageContext.put("uri", uri);

				/**
				 * Evaluate preconditions
				 */
				String acceptHeader = request.getHeader("Accept");
				if(StringUtils.isNotEmpty(acceptHeader)) { //If an Accept header is present, make sure we comply to it!
					if(!listener.accepts(acceptHeader)) {
						response.setStatus(406);
						response.getWriter().print("It appears you expected the MediaType ["+acceptHeader+"] but I only support the MediaType ["+listener.getContentType()+"] :)");
						log.warn(createAbortMessage(remoteUser, 406) + "client expects ["+acceptHeader+"] got ["+listener.getContentType()+"] instead");
						return;
					}
				}

				if(request.getContentType() != null && !listener.isConsumable(request.getContentType())) {
					response.setStatus(415);
					log.warn(createAbortMessage(remoteUser, 415) + "did not match consumes ["+listener.getConsumes()+"] got ["+request.getContentType()+"] instead");
					return;
				}

				String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
				log.debug("Evaluating preconditions for listener["+listener.getName()+"] etagKey["+etagCacheKey+"]");
				if(cache.containsKey(etagCacheKey)) {
					String cachedEtag = (String) cache.get(etagCacheKey);
					log.debug("found etag value["+cachedEtag+"] for key["+etagCacheKey+"]");

					if(method == HttpMethod.GET) {
						String ifNoneMatch = request.getHeader("If-None-Match");
						if(ifNoneMatch != null && ifNoneMatch.equals(cachedEtag)) {
							response.setStatus(304);
							if (log.isDebugEnabled()) log.debug(createAbortMessage(remoteUser, 304) + "matched if-none-match ["+ifNoneMatch+"]");
							return;
						}
					}
					else {
						String ifMatch = request.getHeader("If-Match");
						if(ifMatch != null && !ifMatch.equals(cachedEtag)) {
							response.setStatus(412);
							log.warn(createAbortMessage(remoteUser, 412) + "matched if-match ["+ifMatch+"] method ["+method+"]");
							return;
						}
					}
				}
				messageContext.put(UPDATE_ETAG_CONTEXT_KEY, listener.getUpdateEtag());

				/**
				 * Check authorization
				 */
				//TODO: authentication implementation

				/**
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
						if(log.isTraceEnabled()) log.trace("setting uriSegment ["+name+"] to ["+uriSegments[i]+"]");
						messageContext.put(name, uriSegments[i]);
					}
				}

				/**
				 * Map queryParameters into messageContext
				 */
				Enumeration<String> paramnames = request.getParameterNames();
				while (paramnames.hasMoreElements()) {
					String paramname = paramnames.nextElement();
					String[] paramList = request.getParameterValues(paramname);
					if(paramList.length > 1) { // contains multiple items
						List<String> valueList = Arrays.asList(paramList);
						if(log.isTraceEnabled()) log.trace("setting queryParameter ["+paramname+"] to "+valueList);
						messageContext.put(paramname, valueList);
					}
					else {
						String paramvalue = request.getParameter(paramname);
						if(log.isTraceEnabled()) log.trace("setting queryParameter ["+paramname+"] to ["+paramvalue+"]");
						messageContext.put(paramname, paramvalue);
					}
				}

				/**
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
							log.info("unable to convert header to xml name["+headerParam+"] value["+headerValue+"]");
						}
					}
					messageContext.put("headers", headersXml.toXML());
				}

				/**
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
								if(log.isTraceEnabled()) log.trace("setting multipart formField ["+fieldName+"] to ["+message+"]");
								messageContext.put(fieldName, message.asString());
								attachment.addAttribute("type", "text");
								attachment.addAttribute("value", message.asString());
							} else {
								// Process form file field (input type="file").
								String fieldNameName = fieldName + "Name";
								String fileName = MultipartUtils.getFileName(bodyPart);
								if(log.isTraceEnabled()) log.trace("setting multipart formFile ["+fieldNameName+"] to ["+fileName+"]");
								messageContext.put(fieldNameName, fileName);
								if(log.isTraceEnabled()) log.trace("setting parameter ["+fieldName+"] to input stream of file ["+fileName+"]");
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
						throw new IOException("Could not read mime multipart response", e);
					}
				} else {
					body = MessageUtils.parseContentAsMessage(request);
				}

				/**
				 * Compile Allow header
				 */
				StringBuilder methods = new StringBuilder();
				methods.append("OPTIONS, ");
				for (HttpMethod mtd : config.getMethods()) {
					methods.append(mtd + ", ");
				}
				messageContext.put("allowedMethods", methods.substring(0, methods.length()-2));

				String messageId = null;
				if(StringUtils.isNotEmpty(listener.getMessageIdHeader())) {
					String messageIdHeader = request.getHeader(listener.getMessageIdHeader());
					if(StringUtils.isNotEmpty(messageIdHeader)) {
						messageId = messageIdHeader;
					}
				}
				String correlationId = null;
				if(StringUtils.isNotEmpty(listener.getCorrelationIdHeader())) {
					String correlationIdHeaderValue = request.getHeader(listener.getCorrelationIdHeader());
					if(StringUtils.isNotEmpty(correlationIdHeaderValue)) {
						messageId = correlationIdHeaderValue;
					}
				}
				if (StringUtils.isEmpty(correlationId) && StringUtils.isNotEmpty(messageId)) {
					correlationId = messageId;
				}
				PipeLineSession.setListenerParameters(messageContext, messageId, correlationId, null, null); //We're only using this method to keep setting mid/cid uniform
				Message result = listener.processRequest(body, messageContext);

				/**
				 * Calculate an eTag over the processed result and store in cache
				 */
				Boolean updateEtag = messageContext.getBoolean(UPDATE_ETAG_CONTEXT_KEY);
				if (updateEtag==null) {
					updateEtag=listener.getUpdateEtag();
				}
				if (updateEtag==null) {
					updateEtag=result==null || result.isRepeatable();
				}
				if(updateEtag) {
					log.debug("calculating etags over processed result");
					String cleanPattern = listener.getCleanPattern();
					if(!Message.isEmpty(result) && method == HttpMethod.GET && cleanPattern != null) { //If the data has changed, generate a new eTag
						String eTag = MessageUtils.generateMD5Hash(result);
						if(eTag != null) {
							log.debug("adding/overwriting etag with key[{}] value[{}]", etagCacheKey, eTag);
							cache.put(etagCacheKey, eTag);
							response.addHeader("etag", eTag);
						} else {
							log.debug("skipping etag with key[{}] computed value is null", etagCacheKey);
						}
					}
					else {
						log.debug("removing etag with key[{}]", etagCacheKey);
						cache.remove(etagCacheKey);

						// Not only remove the eTag for the selected resources but also the collection
						String key = ApiCacheManager.getParentCacheKey(listener, uri);
						if(key != null) {
							log.debug("removing parent etag with key[{}]", key);
							cache.remove(key);
						}
					}
				}

				/**
				 * Add headers
				 */
				response.addHeader("Allow", (String) messageContext.get("allowedMethods"));

				MimeType contentType = listener.getContentType();
				if(listener.getProduces() == MediaTypes.ANY) {
					Message parsedContentType = messageContext.getMessage("contentType");
					if(!Message.isEmpty(parsedContentType)) {
						contentType = MimeType.valueOf(parsedContentType.asString());
					} else {
						MimeType providedContentType = MessageUtils.getMimeType(result); // MimeType might be known
						if(providedContentType != null) {
							contentType = providedContentType;
						}
					}
				} else if(listener.getProduces() == MediaTypes.DETECT) {
					MimeType computedContentType = MessageUtils.computeMimeType(result); // Calculate MimeType
					if(computedContentType != null) {
						contentType = computedContentType;
					}
				}
				response.setHeader("Content-Type", contentType.toString());

				if(StringUtils.isNotEmpty(listener.getContentDispositionHeaderSessionKey())) {
					String contentDisposition = messageContext.getMessage(listener.getContentDispositionHeaderSessionKey()).asString();
					if(StringUtils.isNotEmpty(contentDisposition)) {
						log.debug("Setting Content-Disposition header to ["+contentDisposition+"]");
						response.setHeader("Content-Disposition", contentDisposition);
					}
				}

				/**
				 * Check if an exitcode has been defined or if a statuscode has been added to the messageContext.
				 */
				int statusCode = messageContext.get(PipeLineSession.EXIT_CODE_CONTEXT_KEY, 0);
				if(statusCode > 0) {
					response.setStatus(statusCode);
				}

				/**
				 * Finalize the pipeline and write the result to the response
				 */
				if(!Message.isEmpty(result)) {
					if(result.isBinary()) {
						StreamUtil.copyStream(result.asInputStream(), response.getOutputStream(), 4096);
					} else {
						StreamUtil.copyReaderToWriter(result.asReader(), response.getWriter(), 4096, false, false);
					}
				}
				if(log.isTraceEnabled()) log.trace("ApiListenerServlet finished with statusCode ["+statusCode+"] result ["+result+"]");
			}
			catch (Exception e) {
				log.warn("ApiListenerServlet caught exception, will rethrow as ServletException", e);
				try {
					response.reset();
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
				}
				catch (IOException | IllegalStateException ex) {
					log.warn("an error occured while tyring to handle exception ["+e.getMessage()+"]", ex);
					//We're only informing the end user(s), no need to catch this error...
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}
		}
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
