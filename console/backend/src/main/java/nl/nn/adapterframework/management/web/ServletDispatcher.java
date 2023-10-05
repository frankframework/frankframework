/*
   Copyright 2016-2023 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.util.HttpUtils;

/**
 * Main dispatcher for all API resources.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */
@Component
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public class ServletDispatcher extends CXFServlet implements DynamicRegistration.ServletWithParameters {

	private static final long serialVersionUID = 3L;

	private final Logger secLog = LogManager.getLogger("SEC");
	private final Logger log = LogManager.getLogger(this);

	@Value("${iaf-api.enabled:true}")
	private boolean isEnabled;

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		if(!isEnabled) {
			return;
		}

		log.debug("initialize {} servlet", this::getName);
		super.init(servletConfig);
	}

	@Override
	public void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
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
		if(!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("OPTIONS")) {
			secLog.debug("received http request from URI [{}:{}] issued by [{}]", method, request.getRequestURI(), HttpUtils.getCommandIssuedBy(request));
		}

		//TODO add X-Rate-Limit to prevent possible clients to flood the IAF API

		super.invoke(request, response);
	}

	@Override
	public void setBus(Bus bus) {
		if(bus != null) {
			String busInfo = String.format("Successfully created %s with SpringBus [%s]", getName(), bus.getId());
			log.info(busInfo);
			getServletContext().log(busInfo);
		}

		super.setBus(bus);
	}

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		// This event listens to all Spring refresh events.
		// When adding new Spring contexts (with this as a parent) refresh events originating from other contexts will also trigger this method.
		// Since we never want to reinitialize this servlet, we can ignore the 'refresh' event completely!
	}

	@Override
	public String getName() {
		return "IAF-API";
	}

	@Override
	public int loadOnStartUp() {
		return 0;
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
		return "iaf/api/*";
	}

	@Override
	public Map<String, String> getParameters() {
		Map<String, String> parameters = new HashMap<>();
		parameters.put("config-location", "FrankFrameworkApiContext.xml");
		parameters.put("bus", "ff-api-bus");
		return parameters;
	}
}
