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
package nl.nn.adapterframework.lifecycle.servlets;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class URLRequestMatcher implements RequestMatcher {
	private final Set<String> absoluteEndpoints;
	private final Set<String> wildcardEndpoints;
	private final Set<String> excludedEndpoints;

	private boolean a = false;

	public URLRequestMatcher(Set<String> rawEndpointsWithExcludes, boolean a) {
		this.a = a;
		absoluteEndpoints = new HashSet<>();
		wildcardEndpoints = new HashSet<>();
		excludedEndpoints = new HashSet<>();
		for(String endpoint : rawEndpointsWithExcludes) {
			if(endpoint.charAt(0) == '!') {
				excludedEndpoints.add(endpoint.substring(1));
			} else if(endpoint.charAt(endpoint.length()-1) == '*') {
				wildcardEndpoints.add(endpoint.substring(0, endpoint.length()-1));
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

		if(absoluteEndpoints.contains(path)) { //absolute match
			log.trace("absolute match with path [{}]", path);
			return true;
		}

		for(String url : wildcardEndpoints) {
			if(path.startsWith(url)) { //wildcard match
				boolean isExcluded = a && excludedEndpoints.contains(path);
				log.trace("relative match with url [{}] and path [{}] is excluded [{}]", url, path, isExcluded);
				return !isExcluded;
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
		StringBuilder sb = new StringBuilder();
		sb.append("Absolute RequestPatterns ").append(this.absoluteEndpoints);
		sb.append(" Wildcard RequestPatterns ").append(this.wildcardEndpoints);
		sb.append(" Excluded RequestPatterns ").append(this.excludedEndpoints);
		return sb.toString();
	}
}