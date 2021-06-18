/*
   Copyright 2016-2018, 2020 Nationale-Nederlanden, 2021 WeAreFrank!

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
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.lifecycle.IbisApplicationServlet;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Some utilities for working with
 * {@link nl.nn.adapterframework.http.RestListener RestListener}.
 * 
 * @author Peter Leeuwenburgh
 */
public class RestListenerUtils {

	protected static Logger log = LogUtil.getLogger(RestListenerUtils.class);

	public static IbisManager retrieveIbisManager(PipeLineSession session) {
		ServletContext servletContext = (ServletContext) session.get(PipeLineSession.SERVLET_CONTEXT_KEY);
		if (servletContext != null) {
			IbisContext ibisContext = IbisApplicationServlet.getIbisContext(servletContext);
			return ibisContext.getIbisManager();
		}

		return null;
	}

	public static String retrieveRequestURL(PipeLineSession session) {
		HttpServletRequest request = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		if (request != null) {
			return request.getRequestURL().toString();
		}
		return null;
	}

	public static String retrieveSOAPRequestURL(PipeLineSession session) {
		HttpServletRequest request = (HttpServletRequest) session.get(PipeLineSession.HTTP_REQUEST_KEY);
		if (request != null) {
			String url = request.getScheme() + "://" + request.getServerName();
			if(!(request.getScheme().equalsIgnoreCase("http") && request.getServerPort() == 80) && !(request.getScheme().equalsIgnoreCase("https") && request.getServerPort() == 443))
				url += ":" + request.getServerPort();
			url += request.getContextPath() + "/services/";
			return url;
		}
		return null;
	}

	public static void writeToResponseOutputStream(PipeLineSession session, InputStream input) throws IOException {
		OutputStream output = retrieveServletOutputStream(session);
		StreamUtil.copyStream(input, output, 30000);
	}

	private static ServletOutputStream retrieveServletOutputStream(PipeLineSession session) throws IOException {
		HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
		if (response != null) {
			return response.getOutputStream();
		}
		return null;
	}

	public static void setResponseContentType(PipeLineSession session, String contentType) throws IOException {
		HttpServletResponse response = (HttpServletResponse) session.get(PipeLineSession.HTTP_RESPONSE_KEY);
		if (response != null) {
			response.setContentType(contentType);
		}
	}

	public static String formatEtag(String restPath, String uriPattern, int hash) {
		return formatEtag(restPath, uriPattern, ""+hash );
	}

	public static String formatEtag(String restPath, String uriPattern, String hash) {
		return Integer.toOctalString(restPath.hashCode()) + "_" +Integer.toHexString(uriPattern.hashCode()) + "_" + hash;
	}
}
