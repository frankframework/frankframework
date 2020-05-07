/*
Copyright 2016-2019 Integration Partners B.V.

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
package nl.nn.adapterframework.http;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Date;

public class CacheControlFilter implements Filter {

	private Logger log = null;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		//We have nothing to initialize
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		if(log == null)
			log = LogUtil.getLogger(this);

		HttpServletResponse resp = (HttpServletResponse) response;
		HttpServletRequest req = (HttpServletRequest) request;
		//resp.setHeader("Expires", "Tue, 03 Jul 2001 06:00:00 GMT");
		resp.setDateHeader("Last-Modified", new Date().getTime());
		resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, post-check=0, pre-check=0");
		resp.setHeader("Pragma", "no-cache");
		log.trace("disabling cache for uri ["+req.getRequestURI()+"]");
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		//We have nothing to destroy
	}
}