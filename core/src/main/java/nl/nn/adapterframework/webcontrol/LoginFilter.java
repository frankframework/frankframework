/*
   Copyright 2013 Nationale-Nederlanden

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
import java.util.Arrays;
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

import nl.nn.adapterframework.configuration.IbisContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;

import org.apache.log4j.Logger;

/**
 * Simple Java Servlet Filter as security constraints workaround for applicationServerType="TIBCOAMX" and otapStage={"ACC", "PRD"}.
 * 
 * @author Peter Leeuwenburgh
 * @version $Id$
 */

public class LoginFilter implements Filter {
	protected Logger log = LogUtil.getLogger(this);

	protected String applicationServerType;
	protected String otapStage;
	protected final List<String> allowedExtentions = new ArrayList<String>();
	protected final List<String> allowedPaths = new ArrayList<String>();

	public void init(FilterConfig filterConfig) throws ServletException {
		applicationServerType = AppConstants.getInstance().getString(
				IbisContext.APPLICATION_SERVER_TYPE, "");
		otapStage = AppConstants.getInstance()
				.getResolvedProperty("otap.stage");

		String allowedExtentionsString = filterConfig
				.getInitParameter("allowedExtentions");
		if (allowedExtentionsString != null) {
			allowedExtentions.addAll(Arrays.asList(allowedExtentionsString
					.split("\\s+")));
		}

		String allowedPathsString = filterConfig
				.getInitParameter("allowedPaths");
		if (allowedPathsString != null) {
			allowedPaths
					.addAll(Arrays.asList(allowedPathsString.split("\\s+")));
		}
	}

	public void doFilter(ServletRequest servletRequest,
			ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		HttpServletRequest req = (HttpServletRequest) servletRequest;

		if (applicationServerType.equals("TIBCOAMX")
				&& (otapStage.equals("ACC") || otapStage.equals("PRD"))) {
			String path = req.getServletPath();
			if (hasAllowedExtension(path)) {
				filterChain.doFilter(servletRequest, servletResponse);
			} else {
				if (isAllowedPath(path)) {
					filterChain.doFilter(servletRequest, servletResponse);
				} else {
					HttpServletResponse res = (HttpServletResponse) servletResponse;
					res.getWriter().write(
							"<html>Not Allowed (" + path + ")</html>");
				}
			}
		} else {
			filterChain.doFilter(servletRequest, servletResponse);
		}
	}

	private boolean hasAllowedExtension(String path) {
		for (String allowedExtension : allowedExtentions) {
			if (FileUtils.extensionEqualsIgnoreCase(path, allowedExtension)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAllowedPath(String path) {
		for (String allowedPath : allowedPaths) {
			if (path.equals(allowedPath)) {
				return true;
			}
		}
		return false;
	}

	public void destroy() {
	}
}
