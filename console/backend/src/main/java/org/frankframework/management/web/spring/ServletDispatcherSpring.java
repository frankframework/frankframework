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
package org.frankframework.management.web.spring;

import java.io.IOException;

import javax.servlet.MultipartConfigElement;
import javax.servlet.Registration;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.lifecycle.DynamicRegistration;
import org.frankframework.util.HttpUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.DispatcherServlet;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
//@MultipartConfig
public class ServletDispatcherSpring extends DispatcherServlet implements DynamicRegistration.Servlet {

	private static final long serialVersionUID = 1L;

	private final Logger secLog = LogManager.getLogger("SEC");
	private final Logger log = LogManager.getLogger(this);

	@Value("${iaf-api.enabled:true}")
	private boolean isEnabled;


	public ServletDispatcherSpring() {
		setContextConfigLocation(ResourceUtils.CLASSPATH_URL_PREFIX + "/FrankFrameworkApiContext2.xml");
		setDetectAllHandlerMappings(false); //Else it will use the parent's (EnvironmentContext) Spring Integration mapping
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		//don't wire the ApplicationContext,
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		if(!isEnabled) {
			return;
		}
		ServletRegistration.Dynamic servletRegistration = (ServletRegistration.Dynamic) servletConfig.getServletContext().getServletRegistration(getName());
		servletRegistration.setAsyncSupported(true);
		servletRegistration.setMultipartConfig(new MultipartConfigElement(""));

		log.debug("initialize {} servlet", this::getName);
		super.init(servletConfig);
	}

	@Override
	public void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if(!isEnabled) {
			try {
				response.sendError(404, "api backend not enabled");
			} catch (IOException e) {
				log.debug("unable to send 404 error to client", e);
			}

			return;
		}

		/**
		 * Log POST, PUT and DELETE requests
		 */
		final String method = request.getMethod();
		if(!"GET".equalsIgnoreCase(method) && !"OPTIONS".equalsIgnoreCase(method)) {
			secLog.debug("received http request from URI [{}:{}] issued by [{}]", method, request.getRequestURI(), HttpUtils.getCommandIssuedBy(request));
		}

		//TODO add X-Rate-Limit to prevent possible clients to flood the IAF API

		super.doService(request, response);
	}

	@Override
	public String getName() {
		return "IAF-API-SPRING";
	}

	@Override
	public int loadOnStartUp() {
		return 1;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return ALL_IBIS_USER_ROLES;
	}

	@Override
	public String getUrlMapping() {
		return "iaf/spring-api/*";
	}

}
