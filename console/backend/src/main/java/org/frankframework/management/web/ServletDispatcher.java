/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.management.web;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.bus.BusMessageUtils;
import org.springframework.web.servlet.DispatcherServlet;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class ServletDispatcher extends DispatcherServlet {

	private static final long serialVersionUID = 4L;

	private final transient Logger secLog = LogManager.getLogger("SEC");

	public ServletDispatcher() {
		setNamespace(getServletName());
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		ServletRegistration.Dynamic servletRegistration = (ServletRegistration.Dynamic) servletConfig.getServletContext().getServletRegistration(getServletName());
		servletRegistration.setAsyncSupported(true);
		servletRegistration.setMultipartConfig(new MultipartConfigElement(""));

		log.debug("initialize {} servlet", this::getServletName);
		super.init(servletConfig);
	}

	@Override
	public void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		// Log POST, PUT and DELETE requests
		final String method = request.getMethod();
		if (!"GET".equalsIgnoreCase(method) && !"OPTIONS".equalsIgnoreCase(method)) {
			secLog.debug("received http request from URI [{}:{}] issued by [{}]", method, request.getRequestURI(), BusMessageUtils.getUserPrincipalName());
		}

		//TODO add X-Rate-Limit to prevent possible clients to flood the IAF API

		// Exceptions should be caught using a HandlerExceptionResolver
		super.doService(request, response);
	}

	@Override
	public String getServletName() {
		return "IAF-API";
	}
}
