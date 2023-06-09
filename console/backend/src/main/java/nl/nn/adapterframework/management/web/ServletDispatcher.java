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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
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
public class ServletDispatcher extends CXFServlet implements DynamicRegistration.ServletWithParameters {

	private static final long serialVersionUID = 3L;

	private Logger secLog = LogManager.getLogger("SEC");
	private Logger log = LogManager.getLogger(this);

	@Value("${iaf-api.enabled:true}")
	private boolean isEnabled;

	@Value("${iaf-api.cors.allowOrigin:''}")
	private String allowedCorsOrigins; //Defaults to nothing

	@Value("${iaf-api.cors.exposeHeaders:Allow, ETag, Content-Disposition}")
	private String exposedCorsHeaders;

	//TODO: Maybe filter out the methods that are not present on the resource? Till then allow all methods
	@Value("${iaf-api.cors.allowMethods:GET, POST, PUT, DELETE, OPTIONS, HEAD}")
	private String allowedCorsMethods;

	private List<String> allowedCorsDomains =  new ArrayList<>();

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {

		if(!isEnabled) {
			return;
		}

		log.debug("initialize IAFAPI servlet");
		super.init(servletConfig);

		if(StringUtils.isNotEmpty(allowedCorsOrigins)) {
			StringTokenizer tokenizer = new StringTokenizer(allowedCorsOrigins, ",");
			while (tokenizer.hasMoreTokens()) {
				String domain = tokenizer.nextToken();
				if(domain.startsWith("http://")) {
					log.warn("cross side resource domain ["+domain+"] is insecure, it is strongly encouraged to use a secure protocol (HTTPS)");
				}
				if(!domain.startsWith("http://") && !domain.startsWith("https://")) {
					log.error("skipping invalid domain ["+domain+"], domains must start with http(s)://");
					continue;
				}
				allowedCorsDomains.add(domain);
				log.debug("whitelisted CORS domain ["+domain+"]");
			}
		}
	}

	@Override
	public void invoke(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		if(!isEnabled) {
			return;
		}

		final String method = request.getMethod();

		/**
		 * Log POST, PUT and DELETE requests
		 */
		if(!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("OPTIONS")) {
			secLog.info(HttpUtils.getExtendedCommandIssuedBy(request));
		}

		/**
		 * Handle Cross-Origin Resource Sharing
		 */
		String origin = request.getHeader("Origin");
		if (origin == null) {
			// Return standard response if OPTIONS request w/o Origin header
			if(method.equals("OPTIONS")) {
				response.setHeader("Allow", allowedCorsMethods);
				response.setStatus(200);
				return;
			}
		}
		else {
			//Always add the cors headers when the origin has been set
			if(allowedCorsDomains.contains(origin)) {
				response.setHeader("Access-Control-Allow-Origin", origin);

				String requestHeaders = request.getHeader("Access-Control-Request-Headers");
				if (requestHeaders != null)
					response.setHeader("Access-Control-Allow-Headers", requestHeaders);

				response.setHeader("Access-Control-Expose-Headers", exposedCorsHeaders);
				response.setHeader("Access-Control-Allow-Methods", allowedCorsMethods);

				// Allow caching cross-domain permission
				response.setHeader("Access-Control-Max-Age", "3600");
			}
			else {
				//If origin has been set, but has not been whitelisted, report the request in security log.
				secLog.info("host["+request.getRemoteHost()+"] tried to access uri["+request.getPathInfo()+"] with origin["+origin+"] but was blocked due to CORS restrictions");
			}
			//If we pass one of the valid domains, it can be used to spoof the connection
		}

		//TODO add X-Rate-Limit to prevent possible clients to flood the IAF API

		/**
		 * Pass request down the chain, except for OPTIONS
		 */
		if (!method.equals("OPTIONS")) {
			super.invoke(request, response);
		}
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
