/*
Copyright 2017-2019 Integration Partners B.V.

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
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.http.HttpSecurityHandler;
import nl.nn.adapterframework.http.rest.ApiDispatchConfig;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

public class ApiListenerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private final String CHARSET="UTF-8";
	private List<String> IGNORE_HEADERS = Arrays.asList("connection", "transfer-encoding", "content-type");

	protected Logger log = LogUtil.getLogger(this);
	private ApiServiceDispatcher dispatcher = null;
	private IApiCache cache = null;
	private int authTTL = AppConstants.getInstance().getInt("api.auth.token-ttl", 60 * 60 * 24 * 7); //Defaults to 7 days
	private String CorsAllowOrigin = AppConstants.getInstance().getString("api.auth.cors.allowOrigin", "*"); //Defaults to everything
	private String CorsExposeHeaders = AppConstants.getInstance().getString("api.auth.cors.exposeHeaders", "Allow, ETag, Content-Disposition");

	public void init() throws ServletException {
		if (dispatcher == null) {
			dispatcher = ApiServiceDispatcher.getInstance();
		}
		if (cache == null) {
			cache = ApiCacheManager.getInstance();
		}
		super.init();
	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		/**
		 * Initiate and populate messageContext
		 */
		PipeLineSessionBase messageContext = new PipeLineSessionBase();
		messageContext.put(IPipeLineSession.HTTP_REQUEST_KEY, request);
		messageContext.put(IPipeLineSession.HTTP_RESPONSE_KEY, response);
		messageContext.put(IPipeLineSession.SERVLET_CONTEXT_KEY, getServletContext());
		messageContext.setSecurityHandler(new HttpSecurityHandler(request));

		try {
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

			ApiDispatchConfig config = dispatcher.findConfigForUri(uri);
			if(config == null) {
				response.setStatus(404);
				log.trace("Aborting request with status [404], no ApiListener configured for ["+uri+"]");
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
					log.trace("Aborting preflight request with status [200], method ["+method+"]");
					return;
				}
			}

			/**
			 * Get serviceClient
			 */
			ApiListener listener = config.getApiListener(method);
			if(listener == null) {
				response.setStatus(405);
				log.trace("Aborting request with status [405], method ["+method+"] not allowed");
				return;
			}

			log.trace("ApiListenerServlet calling service ["+listener.getName()+"]");

			/**
			 * Check authentication
			 */
			ApiPrincipal userPrincipal = null;

			if(listener.getAuthenticationMethod() != null) {

				String authorizationToken = null;
				Cookie authorizationCookie = null;
				if(listener.getAuthenticationMethod().equals("COOKIE")) {

					Cookie[] cookies = request.getCookies();
					for (Cookie cookie : cookies) {
						if(cookie.getName().equals("authenticationToken")) {
							authorizationToken = cookie.getValue();
							authorizationCookie = cookie;
							authorizationCookie.setPath("/");
						}
					}
				}
				else if(listener.getAuthenticationMethod().equals("HEADER")) {
					authorizationToken = request.getHeader("Authorization");
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
					log.trace("Aborting request with status [401], no (valid) credentials supplied");
					return;
				}

				if(authorizationCookie != null) {
					authorizationCookie.setMaxAge(authTTL);
					response.addCookie(authorizationCookie);
				}
				userPrincipal.updateExpiry();
				userPrincipal.setToken(authorizationToken);
				cache.put(authorizationToken, userPrincipal, authTTL);
				messageContext.put("authorizationToken", authorizationToken);
			}
			//Remove this? it's now available as header value
			messageContext.put("remoteAddr", request.getRemoteAddr());
			if(userPrincipal != null)
				messageContext.put(IPipeLineSession.API_PRINCIPAL_KEY, userPrincipal);
			messageContext.put("uri", uri);

			/**
			 * Evaluate preconditions
			 */
			String accept = request.getHeader("Accept");
			if(accept != null && !accept.isEmpty() && !accept.equals("*/*")) {
				if(!listener.getProduces().equals("ANY") && !accept.contains(listener.getContentType())) {
					response.setStatus(406);
					response.getWriter().print("It appears you expected the MediaType ["+accept+"] but I only support the MediaType ["+listener.getContentType()+"] :)");
					log.trace("Aborting request with status [406], client expects ["+accept+"] got ["+listener.getContentType()+"] instead");
					return;
				}
			}

			if(request.getContentType() != null && !listener.isConsumable(request.getContentType())) {
				response.setStatus(415);
				log.trace("Aborting request with status [415], did not match consumes ["+listener.getConsumes()+"] got ["+request.getContentType()+"] instead");
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
						log.trace("Aborting request with status [304], matched if-none-match ["+ifNoneMatch+"]");
						return;
					}
				}
				else {
					String ifMatch = request.getHeader("If-Match");
					if(ifMatch != null && !ifMatch.equals(cachedEtag)) {
						response.setStatus(412);
						log.trace("Aborting request with status [412], matched if-match ["+ifMatch+"] method ["+method+"]");
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
				if(segment.startsWith("{") && segment.endsWith("}")) {
					String name;
					if(segment.equals("*"))
						name = "uriIdentifier_"+uriIdentifier;
					else
						name = segment.substring(1, segment.length()-1);

					uriIdentifier++;
					log.trace("setting uriSegment ["+name+"] to ["+uriSegments[i]+"]");
					messageContext.put(name, uriSegments[i]);
				}
			}

			/**
			 * Map queryParameters into messageContext
			 */
			Enumeration<?> paramnames = request.getParameterNames();
			while (paramnames.hasMoreElements()) {
				String paramname = (String) paramnames.nextElement();
				String paramvalue = request.getParameter(paramname);

				log.trace("setting queryParameter ["+paramname+"] to ["+paramvalue+"]");
				messageContext.put(paramname, paramvalue);
			}

			/**
			 * Map headers into messageContext
			 */
			Enumeration<String> headers = request.getHeaderNames();
			XmlBuilder headersXml = new XmlBuilder("headers");
			while (headers.hasMoreElements()) {
				String header = headers.nextElement().toLowerCase();
				if(IGNORE_HEADERS.contains(header))
					continue;

				XmlBuilder headerXml = new XmlBuilder(header);
				headerXml.setValue(request.getHeader(header));
				headersXml.addSubElement(headerXml);
			}
			messageContext.put("headers", headersXml.toXML());

			/**
			 * Map multipart parts into messageContext
			 */
			String body = "";
			if (ServletFileUpload.isMultipartContent(request)) {
				DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
				ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);
				servletFileUpload.setHeaderEncoding(CHARSET);
				List<FileItem> items = servletFileUpload.parseRequest(request);
				XmlBuilder attachments = new XmlBuilder("parts");
				int i = 0;
				for (FileItem item : items) {
					//First part -> pipeline input
					if(i == 0) {
						body = Misc.streamToString(item.getInputStream(),"\n",false);
					}
					else {
						XmlBuilder attachment = new XmlBuilder("part");
						String fieldName = item.getFieldName();
						attachment.addAttribute("name", fieldName);
						if (item.isFormField()) {
							// Process regular form field (input type="text|radio|checkbox|etc", select, etc).
							String fieldValue = item.getString();
							log.trace("setting multipart formField ["+fieldName+"] to ["+fieldValue+"]");
							messageContext.put(fieldName, fieldValue);
							attachment.addAttribute("type", "text");
							attachment.addAttribute("value", fieldValue);
						} else {
							// Process form file field (input type="file").
							String fieldNameName = fieldName + "Name";
							String fileName = FilenameUtils.getName(item.getName());
							log.trace("setting multipart formFile ["+fieldNameName+"] to ["+fileName+"]");
							messageContext.put(fieldNameName, fileName);
							log.trace("setting parameter ["+fieldName+"] to input stream of file ["+fileName+"]");
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
					}

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
			//TODO: String correlationId = request.getHeader("message-id");
			String result = listener.processRequest(null, body, messageContext);

			/**
			 * Calculate an eTag over the processed result and store in cache
			 */
			if(messageContext.get("updateEtag", true)) {
				log.debug("calculating etags over processed result");
				String cleanPattern = listener.getCleanPattern();
				if(result != null && method.equals("GET")) {
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

			String contentType = listener.getContentType() + ";charset="+CHARSET;
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
			log.trace("ApiListenerServlet finished with statusCode ["+statusCode+"] result ["+result+"]");
		}
		catch (Exception e) {
			log.warn("ApiListenerServlet caught exception, will rethrow as ServletException", e);
			try {
				response.flushBuffer();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
			}
			catch (IllegalStateException ex) {
				//We're only informing the end user(s), no need to catch this error...
				response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}
	}
}
