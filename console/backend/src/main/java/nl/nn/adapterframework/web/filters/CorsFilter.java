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
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
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

import nl.nn.adapterframework.util.StringUtil;

public class CorsFilter implements Filter {
	private final Logger secLog = LogManager.getLogger("SEC");
	private final Logger log = LogManager.getLogger(this);
	private String localHostAddress;

	@Value("${iaf-api.cors.allowOrigin:'*'}")
	private String allowedCorsOrigins; //Defaults to nothing

	@Value("${iaf-api.cors.exposeHeaders:Allow, ETag, Content-Disposition}")
	private String exposedCorsHeaders;

	//TODO: Maybe filter out the methods that are not present on the resource? Till then allow all methods
	@Value("${iaf-api.cors.allowMethods:GET, POST, PUT, DELETE, OPTIONS, HEAD}")
	private String allowedCorsMethods;

	private List<String> allowedCorsDomains =  new ArrayList<>();

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		try {
			localHostAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			log.warn("unable to determine local host address", e);
		}

		List<String> allowedOrigins = StringUtil.split(allowedCorsOrigins);
		for (String domain : allowedOrigins) {
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

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;

		final String method = request.getMethod();

		/**
		 * Handle Cross-Origin Resource Sharing
		 */
		String origin = request.getHeader("Origin");
		if (origin != null) {
			//Always add the cors headers when the origin has been set
			if(isOriginEqualToLocalHost(origin) || allowedCorsDomains.contains(origin)) {
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
				return; //actually block the request
			}
			//If we pass one of the valid domains, it can be used to spoof the connection
		}

		/**
		 * Pass request down the chain, except for OPTIONS
		 */
		// Return standard response if OPTIONS request w/o Origin header
		if(method.equals("OPTIONS")) {
			response.setHeader("Allow", allowedCorsMethods);
			response.setStatus(200);
		} else {
			chain.doFilter(request, response);
		}
	}

	private boolean isOriginEqualToLocalHost(String originHeader) {
		try {
			InetAddress origin = InetAddress.getByName(new URL(originHeader).getHost());
			return localHostAddress != null && localHostAddress.equals(origin.getHostAddress());
		} catch (UnknownHostException e) {
			log.warn("unable to parse host address", e);
			return false;
		} catch (MalformedURLException e) {
			log.warn("invalid ORIGIN header [{}]", originHeader, e);
			return false;
		}
	}

	@Override
	public void destroy() {
		// nothing to destroy
	}

}
