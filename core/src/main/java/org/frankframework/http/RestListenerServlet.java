/*
   Copyright 2013-2015 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.HttpHeaders;

import lombok.extern.log4j.Log4j2;

import org.frankframework.core.ContextSecurityHandler;
import org.frankframework.core.ISecurityHandler;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.StreamUtil;

/**
 * Servlet that listens for REST requests, and handles them over to the RestServiceDispatcher.
 *
 * @author  Gerrit van Brakel
 */
@Log4j2
@IbisInitializer
public class RestListenerServlet extends AbstractHttpServlet {
	private final String corsAllowOrigin = AppConstants.getInstance().getString("rest.cors.allowOrigin", "*"); //Defaults to everything
	private final String corsExposeHeaders = AppConstants.getInstance().getString("rest.cors.exposeHeaders", "Allow, ETag, Content-Disposition");

	private transient RestServiceDispatcher sd = null;

	@Override
	public void init() throws ServletException {
		super.init();
		if (sd==null) {
			sd= RestServiceDispatcher.getInstance();
		}
	}

	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		String path=request.getPathInfo();
		String restPath=request.getServletPath();
		String body = "";

		if(restPath.contains("rest-public")) {
			response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, corsAllowOrigin);
			String headers = request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
			if (headers != null)
				response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, headers);
			response.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, corsExposeHeaders);

			String pattern = sd.findMatchingPattern(path);
			if(pattern!=null) {
				Map<String, Object> methodConfig = sd.getMethodConfig(pattern, "OPTIONS");
				if (methodConfig == null) { //If set, it means the adapter handles the OPTIONS request
					Iterator<String> iter = sd.getAvailableMethods(pattern).iterator();
					StringBuilder sb = new StringBuilder();
					sb.append("OPTIONS"); //Append preflight OPTIONS request
					while (iter.hasNext()) {
						sb.append(", ").append(iter.next());
					}
					response.setHeader("Access-Control-Allow-Methods", sb.toString());

					if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
						response.setStatus(200);
						//Preflight OPTIONS request should not return any data.
						return;
					}
				}
			}
		}

		String ifNoneMatch = request.getHeader("If-None-Match");
		String ifMatch = request.getHeader("If-Match");
		String contentType = request.getHeader("accept");

		if (log.isTraceEnabled()) log.trace("path [{}] If-Match [{}] If-None-Match [{}] contentType [{}]", path, ifMatch, ifNoneMatch, contentType);

		ISecurityHandler securityHandler = new ContextSecurityHandler();
		try (PipeLineSession messageContext = new PipeLineSession()) {
			messageContext.setSecurityHandler(securityHandler);
			messageContext.put(PipeLineSession.HTTP_METHOD_KEY, request.getMethod());

			Enumeration<String> paramNames = request.getParameterNames();
			while (paramNames.hasMoreElements()) {
				String paramname = paramNames.nextElement();
				String paramvalue = request.getParameter(paramname);
				if (log.isTraceEnabled()) log.trace("setting parameter [{}] to [{}]", paramname, paramvalue);
				messageContext.put(paramname, paramvalue);
			}
			if (!MultipartUtils.isMultipart(request)) {
				body = StreamUtil.streamToString(request.getInputStream(),"\n",false);
			}
			try {
				log.trace("RestListenerServlet calling service [{}]", path);
				Message result = sd.dispatchRequest(restPath, path, request, contentType, body, messageContext, response);

				if(Message.isNull(result) && messageContext.containsKey(PipeLineSession.EXIT_CODE_CONTEXT_KEY) && messageContext.containsKey("validateEtag")) {
					int status = Integer.parseInt( ""+ messageContext.get(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
					response.setStatus(status);
					log.trace("aborted request with status [{}]", status);
					return;
				}

				String etag=(String)messageContext.get("etag");
				if (StringUtils.isNotEmpty(etag))
					response.setHeader("etag", etag);

				int statusCode = 0;
				if(messageContext.containsKey(PipeLineSession.EXIT_CODE_CONTEXT_KEY))
					statusCode = Integer.parseInt( ""+ messageContext.get(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
				if(statusCode > 0)
					response.setStatus(statusCode);

				if (Message.isEmpty(result)) {
					log.trace("RestListenerServlet finished with result set in pipeline");
				} else {
					contentType=messageContext.getString("contentType");
					if (StringUtils.isNotEmpty(contentType)) {
						response.setHeader("Content-Type", contentType);
					}
					String contentDisposition=(String)messageContext.get("contentDisposition");
					if (StringUtils.isNotEmpty(contentDisposition)) {
						response.setHeader("Content-Disposition", contentDisposition);
					}
					String allowedMethods=(String)messageContext.get("allowedMethods");
					if (StringUtils.isNotEmpty(allowedMethods)) {
						response.setHeader("Allow", allowedMethods);
					}

					// Finalize the pipeline and write the result to the response
					writeToResponseStream(response, result);
					log.trace("RestListenerServlet finished with result [{}] etag [{}] contentType [{}] contentDisposition [{}]", result, etag, contentType, contentDisposition);
				}
			} catch (ListenerException e) {
				if (!response.isCommitted()) {
					log.warn("RestListenerServlet caught exception, return internal server error",e);
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
				} else {
					log.warn("RestListenerServlet caught exception, response already committed",e);
					throw new ServletException("RestListenerServlet caught exception, response already committed",e);
				}
			}
		}
	}

	private static void writeToResponseStream(HttpServletResponse response, Message result) throws IOException {
		if (result.isBinary()) {
			try (InputStream in = result.asInputStream()) {
				StreamUtil.copyStream(in, response.getOutputStream(), 4096);
			}
		} else {
			try (Reader reader = result.asReader()) {
				StreamUtil.copyReaderToWriter(reader, response.getWriter(), 4096);
			}
		}
	}

	@Override
	public String getUrlMapping() {
		return "/rest/*";
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return ALL_IBIS_ROLES;
	}
}
