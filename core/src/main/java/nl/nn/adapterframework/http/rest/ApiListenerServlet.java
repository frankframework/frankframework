/*
Copyright 2017 Integration Partners B.V.

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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.http.rest.ApiServiceDispatcher;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

public class ApiListenerServlet extends HttpServlet {
	protected Logger log = LogUtil.getLogger(this);

	protected static final String HTTPREQUESTKEY  = "apiServletRequest";
	protected static final String HTTPRESPONSEKEY = "apiServletResponse";
	protected static final String HTTPCONTEXTKEY  = "apiServletContext";

	private final String CONFIGURATIONKEY_CONTEXT = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);

	private ApiServiceDispatcher dispatcher = null;
	private IApiCache cache = null;
	private ApiAuthorization authorizationManager = null;

	public void init() throws ServletException {
		if (dispatcher == null) {
			dispatcher = ApiServiceDispatcher.getInstance();
		}
		if (cache == null) {
			cache = ApiCacheManager.getInstance();
		}
		if (authorizationManager == null) {
			IbisContext ibisContext = (IbisContext) getServletContext().getAttribute(CONFIGURATIONKEY_CONTEXT);
			String jmsRealm = AppConstants.getInstance().getString("jmsRealm", JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm());
			try {
				authorizationManager = ApiAuthorization.getInstance(ibisContext, jmsRealm);
			} catch (ConfigurationException e) {
				throw new ServletException("failed to initialize api authorizationManager", e);
			}
		}
		super.init();
	}

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		Map<String, Object> messageContext = new HashMap<String, Object>();
		messageContext.put(HTTPREQUESTKEY, request);
		messageContext.put(HTTPRESPONSEKEY, response);
		messageContext.put(HTTPCONTEXTKEY, getServletContext());
		String body = "";

		if (!ServletFileUpload.isMultipartContent(request)) {
			body=Misc.streamToString(request.getInputStream(),"\n",false);
		}

		try {
			String uri = request.getPathInfo();
			String method = request.getMethod().toUpperCase();
			log.trace("ApiListenerServlet dispatching uri ["+uri+"] and method ["+method+"]");

			if(uri.startsWith("/"))
				uri = uri.substring(1);
			if(uri.endsWith("/"))
				uri = uri.substring(0, uri.length()-1);

			ApiDispatchConfig config = dispatcher.findConfigForUri(uri);
			if(config == null) {
				//TODO maybe leave this in for debugging?
//				response.sendError(404, "no ApiListener configured for ["+uri+"]");
				response.setStatus(404);
				log.trace("Aborting request with status [404], no ApiListener configured for ["+uri+"]");
				return;
			}

			ApiListener listener = config.getApiListener(method);
			if(listener == null) {
				response.setStatus(405);
				log.trace("Aborting request with status [405], method ["+method+"] not allowed");
				return;
			}

			log.trace("RestListenerServlet calling service ["+listener.getName()+"]");

			/**
			 * Handle Cross-Origin Resource Sharing
			 */
			if(method.equalsIgnoreCase("OPTIONS")) {
				response.setHeader("Access-Control-Allow-Origin", "*");
				String headers = request.getHeader("Access-Control-Request-Headers");
				if (headers != null)
					response.setHeader("Access-Control-Allow-Headers", headers);
				response.setHeader("Access-Control-Expose-Headers", "ETag, Content-Disposition");
	
				StringBuilder methods = new StringBuilder();
				for (String mtd : config.getMethods()) {
					methods.append(", ").append(mtd);
				}
				response.setHeader("Access-Control-Allow-Methods", methods.toString());
				response.setStatus(200);
				log.trace("Aborting preflight request with status [200], method ["+method+"]");
				return;
			}

			/**
			 * Check authentication
			 */
			ApiPrincipal userPrincipal = null;

			if(request.getHeader("test") != null)
				cache.put(request.getHeader("test"), new ApiPrincipal());

			if(listener.getAuthenticationMethod() != null) {
				String authorizationToken = request.getHeader("Authorization");
				if(cache.containsKey(authorizationToken))
					userPrincipal = (ApiPrincipal) cache.get(authorizationToken);

				if(userPrincipal == null) {
					response.setStatus(401);
					log.trace("Aborting request with status [401], no (valid) credentials supplied");
					return;
				}

//				config.getPermittedRoles?(principal)
			}

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

			String etagCacheKey = ApiCacheManager.buildCacheKey(config.getUriPattern());
			//TODO cache unique per user? how to when multiple users use the same endpoint!?
			if(cache.containsKey(etagCacheKey)) {
				String cachedEtag = (String) cache.get(etagCacheKey);

				if(method.equalsIgnoreCase("GET")) {
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

			/**
			 * Check authorization
			 */
			//TODO: authentication implementation
			Map<String, Boolean> CRUD = authorizationManager.getAllowedMethods(listener.getCleanPattern(), userPrincipal);
			if(userPrincipal != null && (CRUD == null || !CRUD.get(method))) {
				response.setStatus(405);
				log.trace("Aborting request with status [405], method ["+method+"] not allowed");
				return;
			}

			/**
			 * Map uriIdentifiers into messageContext 
			 */
			String patternSegments[] = listener.getUriPattern().split("/");
			String uriSegments[] = uri.split("/");
			int uriIdentifier = 0;
			for (int i = 0; i < patternSegments.length; i++) {
				String segment = patternSegments[i];
				if(segment.startsWith("{") && segment.endsWith("}")) {
					String name = "uriIdentifier:";
					if(segment.equals("*"))
						name += uriIdentifier;
					else
						name += segment.substring(1, segment.length()-1);

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

				log.trace("setting parameter ["+paramname+"] to ["+paramvalue+"]");
				messageContext.put("queryParameter:" + paramname, paramvalue);
			}

			/**
			 * Process the request through the pipeline
			 */
			String result = listener.processRequest(null, body, messageContext);

			/**
			 * Calculate an etag over the processed result and store in cache
			 */
			if(result != null && listener.getGenerateEtag()) {
				String eTag = ApiCacheManager.buildEtag(listener.getUriPattern(), result.hashCode());
				cache.put(etagCacheKey, eTag);
				response.addHeader("etag", eTag);
			}

			/**
			 * Add headers
			 */
			//TODO optional: add injected headers from pipeline?
			StringBuilder methods = new StringBuilder();
			for (String mtd : config.getMethods()) {
				if(userPrincipal != null && CRUD.get(mtd))
					methods.append(mtd + ", ");
			}
			if(methods.length() > 0)
				response.addHeader("Allow", methods.substring(0, methods.length()-2));

			String contentType = listener.getContentType() + "; charset=utf-8";
			if(listener.getProduces().equals("ANY")) {
				contentType = (String) messageContext.get("contentType");
			}
			response.setHeader("Content-Type", contentType);

			/**
			 * Check if an exitcode has been defined or if a statuscode has been added to the messageContext.
			 */
			int statusCode = 0;
			if(messageContext.containsKey("exitcode"))
				statusCode = Integer.parseInt( ""+ messageContext.get("exitcode"));
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
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}
}
