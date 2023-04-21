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
package nl.nn.adapterframework.webcontrol;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;

public class CspFilter implements Filter {
	private ContentSecurityPolicyHeaderWriter cspWriter;

	@Value("${cspheader.reportOnly:false}")
	private boolean reportOnly;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		cspWriter = new ContentSecurityPolicyHeaderWriter();

		List<String> policyDirectives = new LinkedList<>();
		policyDirectives.add("default-src 'self';");
		policyDirectives.add("style-src 'self' https://fonts.googleapis.com/css 'unsafe-inline';");
		policyDirectives.add("font-src 'self' https://fonts.gstatic.com;");
		policyDirectives.add("script-src 'self' 'unsafe-eval' 'nonce-ffVersion' 'sha256-nTT9HlzZYsLZk5BbdhMKiMCvEgbfaqTeueMbRW8r6Ak=';");
		policyDirectives.add("connect-src 'self' https://ibissource.org/iaf/releases/;");
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

		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "SAMEORIGIN");
		response.setHeader("X-XSS-Protection", "1; mode=block");

		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		// nothing to destroy
	}

}
