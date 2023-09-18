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

import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class URLRequestMatcher implements RequestMatcher {
	private final Set<String> endpoints;

	public URLRequestMatcher(Set<String> endpoints) {
		this.endpoints = endpoints;
	}

	@Override
	public boolean matches(HttpServletRequest request) {
		if(endpoints.isEmpty()) {
			return false;
		}

		String path = getRequestPath(request);
		for(String url : endpoints) {
			if(!url.endsWith("*") && url.equals(path)) {//absolute match
				log.trace("absolute match with url [{}] and path [{}]", url, path);
				return true;
			}

			if(path.startsWith(url.substring(0, url.length()-1))) {//relative match
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
			url = StringUtils.isNoneEmpty(url) ? url + pathInfo : pathInfo;
		}
		return url;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("RequestPatterns ").append(this.endpoints);
		return sb.toString();
	}
}