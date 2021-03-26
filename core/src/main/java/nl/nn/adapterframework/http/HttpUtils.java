/*
   Copyright 2015 Nationale-Nederlanden

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

import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

/**
 * Some utilities for working with HTTP.
 * 
 * @author Peter Leeuwenburgh
 */
public class HttpUtils {

	public static String getCommandIssuedBy(HttpServletRequest request) {
		String commandIssuedBy = " remoteHost [" + request.getRemoteHost()
				+ "]";
		commandIssuedBy += " remoteAddress [" + request.getRemoteAddr() + "]";
		commandIssuedBy += " remoteUser [" + request.getRemoteUser() + "]";
		return commandIssuedBy;
	}

	public static String getExtendedCommandIssuedBy(HttpServletRequest request) {
		return getExtendedCommandIssuedBy(request, null);
	}

	public static String getExtendedCommandIssuedBy(HttpServletRequest request,
			List<String> secLogParamNames) {
		return getExtendedCommandIssuedBy(request, secLogParamNames, null);
	}

	public static String getExtendedCommandIssuedBy(HttpServletRequest request,
			List<String> secLogParamNames, String message) {
		String contextPath = request.getContextPath();
		String requestUri = request.getRequestURI();
		String reqUri = StringUtils.substringAfter(requestUri, contextPath);
		if (StringUtils.isEmpty(reqUri)) {
			reqUri = requestUri;
		}
		return ("requestUri [" + reqUri + "] params ["
				+ getParametersAsString(request, secLogParamNames)
				+ "] method [" + request.getMethod() + "]" + getCommandIssuedBy(request))
				+ (message == null ? "" : System.getProperty("line.separator")
						+ message);
	}

	private static String getParametersAsString(HttpServletRequest request,
			List<String> secLogParamNames) {
		String result = "";
		Enumeration paramnames = request.getParameterNames();
		while (paramnames.hasMoreElements()) {
			String paramname = (String) paramnames.nextElement();
			if (secLogParamNames == null || secLogParamNames.contains(paramname)) {
				String paramvalue = request.getParameter(paramname);
				if (StringUtils.isNotEmpty(paramvalue)) {
					if (result.length() > 0) {
						result = result + ",";
					}
					result = result + paramname + "=" + paramvalue;
				}
			}
		}
		return result;
	}
}
