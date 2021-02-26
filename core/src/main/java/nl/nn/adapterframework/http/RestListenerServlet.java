/*
   Copyright 2013-2015 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.http;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Logger;

/**
 * Servlet that listens for REST requests, and handles them over to the RestServiceDispatcher.
 * 
 * @author  Gerrit van Brakel
 */
public class RestListenerServlet extends HttpServlet {
	protected Logger log=LogUtil.getLogger(this);
	private String CorsAllowOrigin = AppConstants.getInstance().getString("rest.cors.allowOrigin", "*"); //Defaults to everything
	private String CorsExposeHeaders = AppConstants.getInstance().getString("rest.cors.exposeHeaders", "Allow, ETag, Content-Disposition");
	
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
				Map methodConfig = sd.getMethodConfig(pattern, "OPTIONS");
				if (methodConfig == null) { //If set, it means the adapter handles the OPTIONS request
					Iterator iter = sd.getAvailableMethods(pattern).iterator();
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
		try (IPipeLineSession messageContext= new PipeLineSessionBase()) {
			messageContext.put(IPipeLineSession.securityHandlerKey, securityHandler);
	
			Enumeration paramnames=request.getParameterNames();
			while (paramnames.hasMoreElements()) {
				String paramname = (String)paramnames.nextElement();
				String paramvalue = request.getParameter(paramname);
				if (log.isTraceEnabled()) log.trace("setting parameter ["+paramname+"] to ["+paramvalue+"]");
				messageContext.put(paramname, paramvalue);
			}
			if (!ServletFileUpload.isMultipartContent(request)) {
				body=Misc.streamToString(request.getInputStream(),"\n",false);
			}
			try {
				log.trace("RestListenerServlet calling service ["+path+"]");
				String result=sd.dispatchRequest(restPath, path, request, contentType, body, messageContext, response, getServletContext());
	
				if(result == null && messageContext.containsKey("exitcode") && messageContext.containsKey("validateEtag")) {
					int status = Integer.parseInt( ""+ messageContext.get("exitcode"));
					response.setStatus(status);
					//TODO: overbodig?
					if(log.isDebugEnabled()) log.trace("aborted request with status ["+status+"]");
					return;
				}
	
				String etag=(String)messageContext.get("etag");
				if (StringUtils.isNotEmpty(etag))
					response.setHeader("etag", etag);
	
				int statusCode = 0;
				if(messageContext.containsKey("exitcode"))
					statusCode = Integer.parseInt( ""+ messageContext.get("exitcode"));
				if(statusCode > 0)
					response.setStatus(statusCode);
	
				if (StringUtils.isEmpty(result)) {
					log.trace("RestListenerServlet finished with result set in pipeline");
				} else {
					contentType=(String)messageContext.get("contentType");
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
					response.getWriter().print(result);
					log.trace("RestListenerServlet finished with result ["+result+"] etag ["+etag+"] contentType ["+contentType+"] contentDisposition ["+contentDisposition+"]");
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
}
