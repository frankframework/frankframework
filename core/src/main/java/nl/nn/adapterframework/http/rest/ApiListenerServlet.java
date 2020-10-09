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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.http.HttpServletBase;
import nl.nn.adapterframework.http.rest.ApiListener.AuthenticationMethods;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Niels Meijer
 *
 */
@IbisInitializer
public class ApiListenerServlet extends HttpServletBase {

	private static final long serialVersionUID = 1L;

	private List<String> IGNORE_HEADERS = Arrays.asList("connection", "transfer-encoding", "content-type", "authorization");

	protected Logger log = LogUtil.getLogger(this);
	private ApiServiceDispatcher dispatcher = null;
	private IApiCache cache = null;
	private int authTTL = AppConstants.getInstance().getInt("api.auth.token-ttl", 60 * 60 * 24 * 7); //Defaults to 7 days
	private String CorsAllowOrigin = AppConstants.getInstance().getString("api.auth.cors.allowOrigin", "*"); //Defaults to everything
	private String CorsExposeHeaders = AppConstants.getInstance().getString("api.auth.cors.exposeHeaders", "Allow, ETag, Content-Disposition");

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
		try (JsonWriter jsonWriter = factory.createWriter(response.getOutputStream(), Charset.forName("UTF-8"))) {
			jsonWriter.write(json);
		}
	}
	
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String uri = request.getPathInfo();
		String method = request.getMethod().toUpperCase();
		log.trace("ApiListenerServlet dispatching uri ["+uri+"] and method ["+method+"]");

		if (uri==null) {
			response.setStatus(400);
			log.warn("Aborting request with status [400], empty uri");
			return;
		}
		if(uri.startsWith("/"))
			uri = uri.substring(1);
		if(uri.endsWith("/"))
			uri = uri.substring(0, uri.length()-1);

		/**
		 * Generate an OpenApi json file
		 */
		if(uri.equalsIgnoreCase("openapi.json")) {
			JsonObject jsonSchema = dispatcher.generateOpenApiJsonSchema();
			returnJson(response, 200, jsonSchema);
			return;
		}

		/**
		 * Generate an OpenApi json file for a set of ApiDispatchConfigs
		 */
		if(uri.endsWith("/openapi.json")) {
			uri = uri.substring(0, uri.length()-"/openapi.json".length());
			List<ApiDispatchConfig> apiConfigs = dispatcher.findMatchingConfigsForUri(uri);
			JsonObject jsonSchema = dispatcher.generateOpenApiJsonSchema(apiConfigs);
			returnJson(response, 200, jsonSchema);
			return;
		}

		/**
		 * Initiate and populate messageContext
		 */
		PipeLineSessionBase messageContext = new PipeLineSessionBase();
		messageContext.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		messageContext.put(IPipeLineSession.HTTP_RESPONSE_KEY, response);
		messageContext.put(IPipeLineSession.SERVLET_CONTEXT_KEY, getServletContext());
		messageContext.setSecurityHandler(new HttpSecurityHandler(request));

		try {
			ApiDispatchConfig config = dispatcher.findConfigForUri(uri);
			if(config == null) {
				response.setStatus(404);
				if(log.isTraceEnabled()) log.trace("Aborting request with status [404], no ApiListener configured for ["+uri+"]");
				return;
			}

			/**
			 * Handle Cross-Origin Resource Sharing
			 * TODO make this work behind loadbalancers/reverse proxies
			 * TODO check if request ip/origin header matches allowOrigin property
			 */
			String origin = request.getHeader("Origin");
			if(method.equals("OPTIONS") || origin != null) {
				response.setHeader("Access-Control-Allow-Origin", CorsAllowOrigin);
				String headers = request.getHeader("Access-Control-Request-Headers");
				if (headers != null)
					response.setHeader("Access-Control-Allow-Headers", headers);
				response.setHeader("Access-Control-Expose-Headers", CorsExposeHeaders);
	
				StringBuilder methods = new StringBuilder();
				for (String mtd : config.getMethods()) {
					methods.append(", ").append(mtd);
				}
				response.setHeader("Access-Control-Allow-Methods", methods.toString());

				//Only cut off OPTIONS (aka preflight) requests
				if(method.equals("OPTIONS")) {
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
				if(log.isTraceEnabled()) log.trace("Aborting request with status [405], method ["+method+"] not allowed");
				return;
			}

			if(log.isTraceEnabled()) log.trace("ApiListenerServlet calling service ["+listener.getName()+"]");

			/**
			 * Check authentication
			 */
			ApiPrincipal userPrincipal = null;

			if(!AuthenticationMethods.NONE.equals(listener.getAuthenticationMethod())) {
				String authorizationToken = null;
				Cookie authorizationCookie = null;

				switch (listener.getAuthenticationMethod()) {
				case COOKIE:
					Cookie[] cookies = request.getCookies();
					if(cookies != null) {
						for (Cookie cookie : cookies) {
							if("authenticationToken".equals(cookie.getName())) {
								authorizationToken = cookie.getValue();
								authorizationCookie = cookie;
								authorizationCookie.setPath("/");
							}
						}
					}
					break;
				case HEADER:
					authorizationToken = request.getHeader("Authorization");
					break;
				case AUTHROLE:
					List<String> roles = listener.getAuthenticationRoles();
					if(roles != null) {
						for (String role : roles) {
							if(request.isUserInRole(role)) {
								userPrincipal = new ApiPrincipal(); //Create a dummy user
								break;
							}
						}
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
						authorizationCookie.setMaxAge(0);
						response.addCookie(authorizationCookie);
					}

					response.setStatus(401);
					if(log.isTraceEnabled()) log.trace("Aborting request with status [401], no (valid) credentials supplied");
					return;
				}

				if(authorizationCookie != null) {
					authorizationCookie.setMaxAge(authTTL);
					response.addCookie(authorizationCookie);
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
				messageContext.put(IPipeLineSession.API_PRINCIPAL_KEY, userPrincipal);
			messageContext.put("uri", uri);

			/**
			 * Evaluate preconditions
			 */
			String acceptHeader = request.getHeader("Accept");
			if(StringUtils.isNotEmpty(acceptHeader)) { //If an Accept header is present, make sure we comply to it!
				if(!listener.accepts(acceptHeader)) {
					response.setStatus(406);
					response.getWriter().print("It appears you expected the MediaType ["+acceptHeader+"] but I only support the MediaType ["+listener.getContentType()+"] :)");
					if(log.isTraceEnabled()) log.trace("Aborting request with status [406], client expects ["+acceptHeader+"] got ["+listener.getContentType()+"] instead");
					return;
				}
			}

			if(request.getContentType() != null && !listener.isConsumable(request.getContentType())) {
				response.setStatus(415);
				if(log.isTraceEnabled()) log.trace("Aborting request with status [415], did not match consumes ["+listener.getConsumes()+"] got ["+request.getContentType()+"] instead");
				return;
			}

			String etagCacheKey = ApiCacheManager.buildCacheKey(uri);
			log.debug("Evaluating preconditions for listener["+listener.getName()+"] etagKey["+etagCacheKey+"]");
			if(cache.containsKey(etagCacheKey)) {
				String cachedEtag = (String) cache.get(etagCacheKey);
				log.debug("found etag value["+cachedEtag+"] for key["+etagCacheKey+"]");

				if(method.equals("GET")) {
					String ifNoneMatch = request.getHeader("If-None-Match");
					if(ifNoneMatch != null && ifNoneMatch.equals(cachedEtag)) {
						response.setStatus(304);
						if(log.isTraceEnabled()) log.trace("Aborting request with status [304], matched if-none-match ["+ifNoneMatch+"]");
						return;
					}
				}
				else {
					String ifMatch = request.getHeader("If-Match");
					if(ifMatch != null && !ifMatch.equals(cachedEtag)) {
						response.setStatus(412);
						if(log.isTraceEnabled()) log.trace("Aborting request with status [412], matched if-match ["+ifMatch+"] method ["+method+"]");
						return;
					}
				}
			}
			messageContext.put("updateEtag", listener.getUpdateEtag());

			/**
			 * Check authorization
			 */
			//TODO: authentication implementation

			/**
			 * Map uriIdentifiers into messageContext 
			 */
			String patternSegments[] = listener.getUriPattern().split("/");
			String uriSegments[] = uri.split("/");
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
				String paramvalue = request.getParameter(paramname);

				if(log.isTraceEnabled()) log.trace("setting queryParameter ["+paramname+"] to ["+paramvalue+"]");
				messageContext.put(paramname, paramvalue);
			}

			/**
			 * Map headers into messageContext
			 */
			Enumeration<String> headers = request.getHeaderNames();
			XmlBuilder headersXml = new XmlBuilder("headers");
			while (headers.hasMoreElements()) {
				String headerName = headers.nextElement().toLowerCase();
				if(IGNORE_HEADERS.contains(headerName))
					continue;

				String headerValue = request.getHeader(headerName);
				try {
					XmlBuilder headerXml = new XmlBuilder("header");
					headerXml.addAttribute("name", headerName);
					headerXml.setValue(headerValue);
					headersXml.addSubElement(headerXml);
				}
				catch (Throwable t) {
					log.info("unable to convert header to xml name["+headerName+"] value["+headerValue+"]");
				}
			}
			messageContext.put("headers", headersXml.toXML());

			/**
			 * Map multipart parts into messageContext
			 */
			String body = "";
			if (ServletFileUpload.isMultipartContent(request)) {
				DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
				ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);
				List<FileItem> items = servletFileUpload.parseRequest(request);
				XmlBuilder attachments = new XmlBuilder("parts");
				int i = 0;
				String multipartBodyName = listener.getMultipartBodyName();
				for (FileItem item : items) {
					String fieldName = item.getFieldName();
					//First part -> pipeline input when multipartBodyName=null
					if((i == 0 && multipartBodyName == null) || fieldName.equalsIgnoreCase(multipartBodyName)) {
						//TODO this is possible because it's been read from disk multiple times, ideally you want to stream it directly!
						body = Misc.streamToString(item.getInputStream(),"\n",false);
					}

					XmlBuilder attachment = new XmlBuilder("part");
					attachment.addAttribute("name", fieldName);
					if (item.isFormField()) {
						// Process regular form field (input type="text|radio|checkbox|etc", select, etc).
						String fieldValue = item.getString();
						if(log.isTraceEnabled()) log.trace("setting multipart formField ["+fieldName+"] to ["+fieldValue+"]");
						messageContext.put(fieldName, fieldValue);
						attachment.addAttribute("type", "text");
						attachment.addAttribute("value", fieldValue);
					} else {
						// Process form file field (input type="file").
						String fieldNameName = fieldName + "Name";
						String fileName = FilenameUtils.getName(item.getName());
						if(log.isTraceEnabled()) log.trace("setting multipart formFile ["+fieldNameName+"] to ["+fileName+"]");
						messageContext.put(fieldNameName, fileName);
						if(log.isTraceEnabled()) log.trace("setting parameter ["+fieldName+"] to input stream of file ["+fileName+"]");
						messageContext.put(fieldName, item.getInputStream());

						attachment.addAttribute("type", "file");
						attachment.addAttribute("filename", fileName);
						attachment.addAttribute("size", item.getSize());
						attachment.addAttribute("sessionKey", fieldName);
						String contentType = item.getContentType();
						if(contentType != null) {
							String mimeType = contentType;
							int semicolon = contentType.indexOf(";");
							if(semicolon >= 0) {
								mimeType = contentType.substring(0, semicolon);
								String mightContainCharSet = contentType.substring(semicolon+1).trim();
								if(mightContainCharSet.contains("charset=")) {
									String charSet = mightContainCharSet.substring(mightContainCharSet.indexOf("charset=")+8);
									attachment.addAttribute("charSet", charSet);
								}
							}
							else {
								mimeType = contentType;
							}
							attachment.addAttribute("mimeType", mimeType);
						}
					}
					attachments.addSubElement(attachment);

					i++;
				}
				messageContext.put("multipartAttachments", attachments.toXML());
			}

			/**
			 * Compile Allow header
			 */
			StringBuilder methods = new StringBuilder();
			methods.append("OPTIONS, ");
			for (String mtd : config.getMethods()) {
				methods.append(mtd + ", ");
			}
			messageContext.put("allowedMethods", methods.substring(0, methods.length()-2));

			/**
			 * Process the request through the pipeline
			 */
			if (!ServletFileUpload.isMultipartContent(request)) {
				body = Misc.streamToString(request.getInputStream(),"\n",false);
			}

			String messageId = null;
			if(StringUtils.isNotEmpty(listener.getMessageIdHeader())) {
				String messageIdHeader = request.getHeader(listener.getMessageIdHeader());
				if(StringUtils.isNotEmpty(messageIdHeader)) {
					messageId = messageIdHeader;
				}
			}
			PipeLineSessionBase.setListenerParameters(messageContext, messageId, null, null, null); //We're only using this method to keep setting id/cid/tcid uniform
			String result = listener.processRequest(null, body, messageContext);

			/**
			 * Calculate an eTag over the processed result and store in cache
			 */
			if(messageContext.get("updateEtag", true)) {
				log.debug("calculating etags over processed result");
				String cleanPattern = listener.getCleanPattern();
				if(result != null && method.equals("GET") && cleanPattern != null) {
					String eTag = ApiCacheManager.buildEtag(cleanPattern, result.hashCode());
					log.debug("adding/overwriting etag with key["+etagCacheKey+"] value["+eTag+"]");
					cache.put(etagCacheKey, eTag);
					response.addHeader("etag", eTag);
				}
				else {
					log.debug("removing etag with key["+etagCacheKey+"]");
					cache.remove(etagCacheKey);

					// Not only remove the eTag for the selected resources but also the collection
					String key = ApiCacheManager.getParentCacheKey(listener, uri);
					if(key != null) {
						log.debug("removing parent etag with key["+key+"]");
						cache.remove(key);
					}
				}
			}

			/**
			 * Add headers
			 */
			response.addHeader("Allow", (String) messageContext.get("allowedMethods"));

			String contentType = listener.getContentType();
			if(listener.getProduces().equals("ANY")) {
				contentType = messageContext.get("contentType", contentType);
			}
			response.setHeader("Content-Type", contentType);

			/**
			 * Check if an exitcode has been defined or if a statuscode has been added to the messageContext.
			 */
			int statusCode = messageContext.get("exitcode", 0);
			if(statusCode > 0)
				response.setStatus(statusCode);

			/**
			 * Finalize the pipeline and write the result to the response
			 */
			if(result != null)
				response.getWriter().print(result);
			if(log.isTraceEnabled()) log.trace("ApiListenerServlet finished with statusCode ["+statusCode+"] result ["+result+"]");
		}
		catch (Exception e) {
			log.warn("ApiListenerServlet caught exception, will rethrow as ServletException", e);
			try {
				response.reset();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			catch (IllegalStateException ex) {
				//We're only informing the end user(s), no need to catch this error...
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}

	@Override
	public String getUrlMapping() {
		return "/api/*";
	}
}
