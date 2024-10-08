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
package org.frankframework.console.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;

public class CspFilter implements Filter {
	private ContentSecurityPolicyHeaderWriter cspWriter;

	@Value("${cspheader.reportOnly:false}")
	private boolean reportOnly;

	@Override
	public void init(FilterConfig filterConfig) {
		cspWriter = new ContentSecurityPolicyHeaderWriter();

		List<String> policyDirectives = new ArrayList<>();
		policyDirectives.add("default-src 'self';");
		policyDirectives.add("style-src 'self' https://fonts.googleapis.com/css 'unsafe-inline';");
		policyDirectives.add("font-src 'self' https://fonts.gstatic.com;");
		policyDirectives.add("script-src 'self' 'unsafe-eval' 'nonce-IE-warning' 'sha256-nTT9HlzZYsLZk5BbdhMKiMCvEgbfaqTeueMbRW8r6Ak=';");
		policyDirectives.add("connect-src 'self' ws: wss: https://ibissource.org/iaf/releases/;");
		policyDirectives.add("img-src 'self' data:;");
		policyDirectives.add("frame-ancestors 'self';");
		policyDirectives.add("form-action 'none';");
		// 'sha256-nTT9HlzZYsLZk5BbdhMKiMCvEgbfaqTeueMbRW8r6Ak=' belongs to larva

		cspWriter.setPolicyDirectives(StringUtils.join(policyDirectives, " "));
		cspWriter.setReportOnly(reportOnly);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
		HttpServletResponse response = (HttpServletResponse) resp;
		cspWriter.writeHeaders((HttpServletRequest) request, response);
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// nothing to destroy
	}

}
