/*
Copyright 2020-2021 WeAreFrank - Information Technology & Services.

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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.apache.log4j.Logger;

import nl.nn.adapterframework.http.HttpUtils;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.LogUtil;

public class ServletDispatcher extends CXFServlet {
	

	private static final long serialVersionUID = 1L;

	private Logger secLog = LogUtil.getLogger("SEC");
	private Logger log = LogUtil.getLogger(this);
	private AppConstants appConstants = AppConstants.getInstance();

	private final boolean IAF_API_ENABLED = appConstants.getBoolean("iaf-api.enabled", true);
	private final String CORS_ALLOW_ORIGIN = appConstants.getString("iaf-api.cors.allowOrigin", ""); //Defaults to nothing
	private final String CORS_EXPOSE_HEADERS = appConstants.getString("iaf-api.cors.exposeHeaders", "Allow, ETag, Content-Disposition");
	//TODO: Maybe filter out the methods that are not present on the resource? Till then allow all methods
	private final String CORS_ALLOW_METHODS = appConstants.getString("iaf-api.cors.allowMethods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");

	private List<String> allowedCorsDomains =  new ArrayList<String>();
	private String mappingPrefix = "";

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {

		if(!IAF_API_ENABLED) {
			return;
		}

		log.debug("initialize IAFAPI servlet");
		super.init(servletConfig);

		if(log.isDebugEnabled()) {
			StringTokenizer resources = new StringTokenizer(getInitParameter("rest.services"), ",");
			while (resources.hasMoreTokens()) {
				String resource = resources.nextToken();
				log.debug("loading resource["+resource.trim()+"]");
			}
		}
		mappingPrefix = getInitParameter("rest.servlet.mapping.prefix");

		if(!CORS_ALLOW_ORIGIN.isEmpty()) {
			StringTokenizer tokenizer = new StringTokenizer(CORS_ALLOW_ORIGIN, ",");
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
	public void service(ServletRequest req, ServletResponse resp) throws ServletException, IOException {

		if(!IAF_API_ENABLED) {
			return;
		}

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		final String method = request.getMethod();
		/**
		 * Log POST, PUT and DELETE requests
		 */
		if(!method.equalsIgnoreCase("GET") && !method.equalsIgnoreCase("OPTIONS"))
			secLog.info(HttpUtils.getExtendedCommandIssuedBy(request));

		/**
		 * Handle Cross-Origin Resource Sharing
		 */
		String origin = request.getHeader("Origin");
		if (origin == null) {
			// Return standard response if OPTIONS request w/o Origin header
			if(method.equals("OPTIONS")) {
				response.setHeader("Allow", CORS_ALLOW_METHODS);
				response.setStatus(200);
				return;
			}
		}
		else {
			//Always add the cors headers when the origin has been set
			allowedCorsDomains.stream().filter(s -> allowedCorsDomains.contains(origin)).findAny().map(s -> {
				response.setHeader("Access-Control-Allow-Origin", origin);
				String requestHeaders = request.getHeader("Access-Control-Request-Headers");
				if (requestHeaders != null)
					response.setHeader("Access-Control-Allow-Headers", requestHeaders);

				response.setHeader("Access-Control-Expose-Headers", CORS_EXPOSE_HEADERS);
				response.setHeader("Access-Control-Allow-Methods", CORS_ALLOW_METHODS);

				// Allow caching cross-domain permission
				response.setHeader("Access-Control-Max-Age", "3600");
				return null;
			}).orElseGet(() -> {
				secLog.info("host[" + request.getRemoteHost() + "] tried to access uri[" + mappingPrefix
						+ request.getPathInfo() + "] with origin[" + origin
						+ "] but was blocked due to CORS restrictions");
				return null;
			});
			
			//If we pass one of the valid domains, it can be used to spoof the connection
		}

		//TODO add X-Rate-Limit to prevent possible clients to flood the IAF API

		/**
		 * Pass request down the chain, except for OPTIONS
		 */
		if (!method.equals("OPTIONS")) {
			super.service(request, response);
		}
	}
    
    
	
}
