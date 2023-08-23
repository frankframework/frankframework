/*
   Copyright 2022-2023 WeAreFrank!

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
package nl.nn.adapterframework.web.filters;

import java.io.IOException;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;

import nl.nn.adapterframework.util.StringUtil;

public class CorsFilter implements Filter {
	private final Logger secLog = LogManager.getLogger("SEC");
	private final Logger log = LogManager.getLogger(this);

	@Value("${iaf-api.cors.allowOrigin:*}")
	private String allowedCorsOrigins; //Defaults to ALL allowed

	@Value("${iaf-api.cors.exposeHeaders:Allow, ETag, Content-Disposition}")
	private String exposedCorsHeaders;

	//TODO: Maybe filter out the methods that are not present on the resource? Till then allow all methods
	@Value("${iaf-api.cors.allowMethods:GET, POST, PUT, DELETE, OPTIONS, HEAD}")
	private String allowedCorsMethods;

	@Value("${iaf-api.cors.enforced:false}")
	private boolean enforceCORS;

	private CorsConfiguration config = new CorsConfiguration();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		List<String> allowedOrigins = StringUtil.split(allowedCorsOrigins);
		for (String domain : allowedOrigins) {
			if("*".equals(domain) || !domain.contains("*")) {
				config.addAllowedOrigin(domain);
			} else {
				config.addAllowedOriginPattern(domain);
			}
		}
		log.debug("whitelisted CORS origins: {} and patterns: {}", config::getAllowedOrigins, config::getAllowedOriginPatterns);
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		/**
		 * Handle Cross-Origin Resource Sharing
		 */
		if(enforceCORS && CorsUtils.isCorsRequest(request)) {
			String originHeader = request.getHeader("Origin");
			String origin = config.checkOrigin(originHeader);
			if (origin != null) {
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
				secLog.info("host["+request.getRemoteHost()+"] tried to access uri["+request.getPathInfo()+"] with origin["+originHeader+"] but was blocked due to CORS restrictions");
				log.warn("blocked request with origin [{}]", originHeader);
				response.setStatus(500);
				return; //actually block the request
			}
			//If we pass one of the valid domains, it can be used to spoof the connection
		}

		/**
		 * Pass request down the chain, except for OPTIONS
		 */
		// Return standard response if OPTIONS request w/o Origin header
		if(CorsUtils.isPreFlightRequest(request)) {
			response.setHeader("Allow", allowedCorsMethods);
			response.setStatus(200);
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		// nothing to destroy
	}

}
