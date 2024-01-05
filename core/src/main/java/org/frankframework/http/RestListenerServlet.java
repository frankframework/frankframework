/*
   Copyright 2013-2015 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import org.frankframework.core.ISecurityHandler;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.http.mime.MultipartUtils;
import org.frankframework.lifecycle.IbisInitializer;
import org.frankframework.stream.Message;
import org.frankframework.util.AppConstants;
import org.frankframework.util.LogUtil;

import org.frankframework.util.StreamUtil;

/**
 * Servlet that listens for REST requests, and handles them over to the RestServiceDispatcher.
 *
 * @author  Gerrit van Brakel
 */
@IbisInitializer
public class RestListenerServlet extends HttpServletBase {
	protected Logger log=LogUtil.getLogger(this);
	private final String CorsAllowOrigin = AppConstants.getInstance().getString("rest.cors.allowOrigin", "*"); //Defaults to everything
	private final String CorsExposeHeaders = AppConstants.getInstance().getString("rest.cors.exposeHeaders", "Allow, ETag, Content-Disposition");

	private RestServiceDispatcher sd=null;

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
			response.setHeader("Access-Control-Allow-Origin", CorsAllowOrigin);
			String headers = request.getHeader("Access-Control-Request-Headers");
			if (headers != null)
				response.setHeader("Access-Control-Allow-Headers", headers);
			response.setHeader("Access-Control-Expose-Headers", CorsExposeHeaders);

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

		String ifNoneMatch=request.getHeader("If-None-Match");
		String ifMatch=request.getHeader("If-Match");
		String contentType=request.getHeader("accept");

		if (log.isTraceEnabled()) log.trace("path ["+path+"] If-Match ["+ifMatch+"] If-None-Match ["+ifNoneMatch+"] contentType ["+contentType+"]");

		ISecurityHandler securityHandler = new HttpSecurityHandler(request);
		try (PipeLineSession messageContext= new PipeLineSession()) {
			messageContext.setSecurityHandler(securityHandler);

			Enumeration<String> paramnames=request.getParameterNames();
			while (paramnames.hasMoreElements()) {
				String paramname = paramnames.nextElement();
				String paramvalue = request.getParameter(paramname);
				if (log.isTraceEnabled()) log.trace("setting parameter ["+paramname+"] to ["+paramvalue+"]");
				messageContext.put(paramname, paramvalue);
			}
			if (!MultipartUtils.isMultipart(request)) {
				body = StreamUtil.streamToString(request.getInputStream(),"\n",false);
			}
			try {
				log.trace("RestListenerServlet calling service ["+path+"]");
				Message result = sd.dispatchRequest(restPath, path, request, contentType, body, messageContext, response, getServletContext());

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

					/*
					 * Finalize the pipeline and write the result to the response
					 */
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
