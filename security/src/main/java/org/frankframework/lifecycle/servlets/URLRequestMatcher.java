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
package org.frankframework.lifecycle.servlets;

import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class URLRequestMatcher implements RequestMatcher {
	private final Set<String> absoluteEndpoints;
	private final Set<String> wildcardEndpoints;

	public URLRequestMatcher(Set<String> rawEndpointsWithExcludes) {
		absoluteEndpoints = new HashSet<>();
		wildcardEndpoints = new HashSet<>();
		for(String endpoint : rawEndpointsWithExcludes) {
			if(endpoint.charAt(0) == '!') {
				throw new IllegalArgumentException("endpoint may not start with [!]");
			}
			if(endpoint.charAt(endpoint.length()-1) == '*') {
				wildcardEndpoints.add(endpoint.substring(0, endpoint.length()-1));
				if (endpoint.charAt(endpoint.length() - 2) == '/') { // Add endpoint(s) without `/*` to absolute path (ApiListenerServlet, IAF-API etc)
					absoluteEndpoints.add(endpoint.substring(0, endpoint.length() - 2));
				}
			} else {
				absoluteEndpoints.add(endpoint);
			}
		}

		if(absoluteEndpoints.isEmpty() && wildcardEndpoints.isEmpty()) {
			throw new IllegalArgumentException("must provided at least 1 endpoint");
		}
	}

	@Override
	public boolean matches(HttpServletRequest request) {
		String path = getRequestPath(request);

		if (absoluteEndpoints.contains(path)) { // absolute match
			log.trace("absolute match with path [{}]", path);
			return true;
		}

		for(String url : wildcardEndpoints) {
			if (path.startsWith(url)) { // wildcard match
				log.trace("relative match with url [{}] and path [{}]", url, path);
				return true;
			}
		}

		return false;
	}

	private String getRequestPath(HttpServletRequest request) {
		String url = request.getServletPath();
		String pathInfo = request.getPathInfo();
		if (pathInfo != null) {
			url = StringUtils.isNotEmpty(url) ? url + pathInfo : pathInfo;
		}
		return url;
	}

	@Override
	public String toString() {
		return "Absolute RequestPatterns " + absoluteEndpoints +
				" Wildcard RequestPatterns " + wildcardEndpoints;
	}
}
