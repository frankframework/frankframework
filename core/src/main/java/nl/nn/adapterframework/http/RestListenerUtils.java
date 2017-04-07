/*
   Copyright 2016 Nationale-Nederlanden

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
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.webcontrol.ConfigurationServlet;

/**
 * Some utilities for working with
 * {@link nl.nn.adapterframework.http.RestListener RestListener}.
 * 
 * @author Peter Leeuwenburgh
 */
public class RestListenerUtils {
	public static final String REST_LISTENER_SERVLET_REQUEST = "restListenerServletRequest";
	public static final String REST_LISTENER_SERVLET_RESPONSE = "restListenerServletResponse";
	public static final String REST_LISTENER_SERVLET_CONTEXT = "restListenerServletContext";

	public static IbisManager retrieveIbisManager(IPipeLineSession session) {
		ServletContext servletContext = (ServletContext) session.get(REST_LISTENER_SERVLET_CONTEXT);
		if (servletContext != null) {
			String attributeKey = AppConstants.getInstance().getProperty(ConfigurationServlet.KEY_CONTEXT);
			IbisContext ibisContext = (IbisContext) servletContext.getAttribute(attributeKey);
			if (ibisContext != null) {
				return ibisContext.getIbisManager();
			}
		}
		return null;
	}

	public static ServletOutputStream retrieveServletOutputStream(IPipeLineSession session) throws IOException {
		HttpServletResponse response = (HttpServletResponse) session.get(REST_LISTENER_SERVLET_RESPONSE);
		if (response != null) {
			return response.getOutputStream();
		}
		return null;
	}

	public static String retrieveRequestURL(IPipeLineSession session) throws IOException {
		HttpServletRequest request = (HttpServletRequest) session.get(REST_LISTENER_SERVLET_REQUEST);
		if (request != null) {
			return request.getRequestURL().toString();
		}
		return null;
	}

	public static String retrieveSOAPRequestURL(IPipeLineSession session) throws IOException {
		HttpServletRequest request = (HttpServletRequest) session.get(REST_LISTENER_SERVLET_REQUEST);
		if (request != null) {
			String url = request.getScheme() + "://" + request.getServerName();
			if(!(request.getScheme().equalsIgnoreCase("http") && request.getServerPort() == 80) && !(request.getScheme().equalsIgnoreCase("https") && request.getServerPort() == 443))
				url += ":" + request.getServerPort();
			url += request.getContextPath() + "/servlet/rpcrouter/";
			return url;
		}
		return null;
	}

	public static void writeToResponseOutputStream(IPipeLineSession session, byte[] bytes) throws IOException {
		retrieveServletOutputStream(session).write(bytes);
	}

	public static void setResponseContentType(IPipeLineSession session, String contentType) throws IOException {
		HttpServletResponse response = (HttpServletResponse) session.get(REST_LISTENER_SERVLET_RESPONSE);
		if (response != null) {
			response.setContentType(contentType);
		}
	}

	public static String retrieveRequestRemoteUser(IPipeLineSession session) throws IOException {
		HttpServletRequest request = (HttpServletRequest) session.get(REST_LISTENER_SERVLET_REQUEST);
		if (request != null) {
			Principal principal = request.getUserPrincipal();
			if (principal != null) {
				return principal.getName();
			}
		}
		return null;
	}
}
