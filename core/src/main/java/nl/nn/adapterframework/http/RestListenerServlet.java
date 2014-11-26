/*
   Copyright 2013 Nationale-Nederlanden

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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISecurityHandler;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Servlet that listens for REST requests, and handles them over to the RestServiceDispatcher.
 * 
 * @author  Gerrit van Brakel
 */
public class RestListenerServlet extends HttpServlet {
	protected Logger log=LogUtil.getLogger(this);
	
	private RestServiceDispatcher sd=null;
	
	public void init() throws ServletException {
		super.init();
		if (sd==null) {
			sd= RestServiceDispatcher.getInstance();
		}
	}
	
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		String path=request.getPathInfo();
		String body = "";
		if (request.getMethod().equalsIgnoreCase("GET")) {
			body=Misc.streamToString(request.getInputStream(),"\n",false);
		}
			
		String etag=request.getHeader("etag");
		String contentType=request.getHeader("accept");

		if (log.isDebugEnabled()) log.debug("path ["+path+"] etag ["+etag+"] contentType ["+contentType+"]");
		
		ISecurityHandler securityHandler = new HttpSecurityHandler(request);
		Map messageContext= new HashMap();
		messageContext.put(IPipeLineSession.securityHandlerKey, securityHandler);

		Enumeration paramnames=request.getParameterNames();
		while (paramnames.hasMoreElements()) {
			String paramname = (String)paramnames.nextElement();
			String paramvalue = request.getParameter(paramname);
			if (log.isDebugEnabled()) log.debug("setting parameter ["+paramname+"] to ["+paramvalue+"]");
			messageContext.put(paramname, paramvalue);
		}
		if (request.getMethod().equalsIgnoreCase("POST")) {
			try {
				DiskFileItemFactory diskFileItemFactory = new DiskFileItemFactory();
				ServletFileUpload servletFileUpload = new ServletFileUpload(diskFileItemFactory);
				List<FileItem> items = servletFileUpload.parseRequest(request);
		        for (FileItem item : items) {
		            if (item.isFormField()) {
		                // Process regular form field (input type="text|radio|checkbox|etc", select, etc).
		                String fieldName = item.getFieldName();
		                String fieldValue = item.getString();
		    			if (log.isDebugEnabled()) log.debug("setting parameter ["+fieldName+"] to ["+fieldValue+"]");
		    			messageContext.put(fieldName, fieldValue);
		            } else {
		                // Process form file field (input type="file").
		                String fieldName = item.getFieldName();
		                String fieldNameName = fieldName + "Name";
		                String fileName = FilenameUtils.getName(item.getName());
		    			if (log.isDebugEnabled()) log.debug("setting parameter ["+fieldNameName+"] to ["+fileName+"]");
		    			messageContext.put(fieldNameName, fileName);
		                InputStream inputStream = item.getInputStream();
		    			if (log.isDebugEnabled()) log.debug("setting parameter ["+fieldName+"] to input stream of file ["+fileName+"]");
		    			messageContext.put(fieldName, inputStream);
		            }
		        }
			} catch (FileUploadException e) {
				log.warn("RestListenerServlet caught FileUploadException, will rethrow as ServletException",e);
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
			}
		}
		try {
			log.debug("RestListenerServlet calling service ["+path+"]");
			String result=sd.dispatchRequest(path, request.getMethod(), etag, contentType, body, messageContext, response);
			if (StringUtils.isEmpty(result)) {
				log.debug("RestListenerServlet finished with result set in pipeline");
			} else {
				etag=(String)messageContext.get("etag");
				contentType=(String)messageContext.get("contentType");
				if (StringUtils.isNotEmpty(contentType)) { 
					response.setHeader("Content-Type", contentType); 
				}
				String contentDisposition=(String)messageContext.get("contentDisposition");
				if (StringUtils.isNotEmpty(contentDisposition)) { 
					response.setHeader("Content-Disposition", contentDisposition); 
				}
				if (StringUtils.isNotEmpty(etag)) { 
					response.setHeader("etag", etag); 
				}
				response.getWriter().print(result);
				log.debug("RestListenerServlet finished with result ["+result+"] etag ["+etag+"] contentType ["+contentType+"] contentDisposition ["+contentDisposition+"]");
			}
		} catch (ListenerException e) {
			log.warn("RestListenerServlet caught exception, will rethrow as ServletException",e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,e.getMessage());
		}
	}
	
}
