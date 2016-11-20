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
package nl.nn.adapterframework.webcontrol.api;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
* Add CORS header info.
* 
* @author	Niels Meijer
*/

public class CorsFilter implements Filter {
	public static String VALID_METHODS = "GET, POST, PUT, DELETE, OPTIONS, HEAD";

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

		HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String origin = req.getHeader("Origin");
        if (origin == null) {
            // Return standard response if OPTIONS request w/o Origin header
            if (req.getMethod().equalsIgnoreCase("OPTIONS")) {
                resp.setHeader("Allow", VALID_METHODS);
                resp.setStatus(200);
                return;
            }
        }
        else {
            // This is a cross-domain request, add headers allowing access
            resp.setHeader("Access-Control-Allow-Origin", origin);
            resp.setHeader("Access-Control-Allow-Methods", VALID_METHODS);
            //resp.setHeader("Access-Control-Allow-Headers", "X-PINGOTHER, Origin, X-Requested-With, Content-Type, Accept, Authorization, If-None-Match");
            resp.setHeader("Access-Control-Expose-Headers", "ETag, Content-Disposition");

            String headers = req.getHeader("Access-Control-Request-Headers");
            if (headers != null)
            	resp.setHeader("Access-Control-Allow-Headers", headers);

            // Allow caching cross-domain permission
            resp.setHeader("Access-Control-Max-Age", "3600");
        }
        // Pass request down the chain, except for OPTIONS
        if (!req.getMethod().equalsIgnoreCase("OPTIONS")) {
        	chain.doFilter(req, resp);
        }
    }

	public void init(FilterConfig filterConfig) throws ServletException {
		// TODO Auto-generated method stub
	}

	public void destroy() {
		// TODO Auto-generated method stub
	}
}