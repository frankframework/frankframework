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
package org.frankframework.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import lombok.extern.log4j.Log4j2;

/**
 * Some utilities for working with HTTP.
 *
 * @author Peter Leeuwenburgh
 */
@Log4j2
public class HttpUtils {

	private static String getCommandIssuedBy(HttpServletRequest request) {
		String commandIssuedBy = " remoteHost [" + request.getRemoteHost() + "]";
		commandIssuedBy += " remoteAddress [" + request.getRemoteAddr() + "]";
		commandIssuedBy += " remoteUser [" + StringEscapeUtils.escapeJava(request.getRemoteUser()) + "]";
		return commandIssuedBy;
	}

	public static String getExtendedCommandIssuedBy(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		String requestUri = request.getRequestURI();
		String reqUri = StringUtils.substringAfter(requestUri, contextPath);
		if (StringUtils.isEmpty(reqUri)) {
			reqUri = requestUri;
		}
		return ("requestUri [" + reqUri + "] params [" + getParametersAsString(request)
				+ "] method [" + request.getMethod() + "]" + getCommandIssuedBy(request));
	}

	private static String getParametersAsString(HttpServletRequest request) {
		String result = "";
		Enumeration<String> paramnames = request.getParameterNames();
		while (paramnames.hasMoreElements()) {
			String paramname = StringEscapeUtils.escapeJava(paramnames.nextElement());
			String paramvalue = StringEscapeUtils.escapeJava(request.getParameter(paramname));
			if (StringUtils.isNotEmpty(paramvalue)) {
				if (result.length() > 0) {
					result = result + ",";
				}
				result = result + paramname + "=" + paramvalue;
			}
		}
		return result;
	}

	public static String urlDecode(String input) {
		try {
			return URLDecoder.decode(input, StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
		} catch (UnsupportedEncodingException e) {
			log.warn("unable to decode input using charset [{}]", StreamUtil.DEFAULT_INPUT_STREAM_ENCODING, e);
			throw new IllegalArgumentException(e);
		}
	}

	public static String decodeBase64(String input) {
		try {
			byte[] decoded = Base64.getDecoder().decode(input);
			return new String(decoded, StandardCharsets.UTF_8);
		} catch (IllegalArgumentException e) {
			log.warn("unable to decode input string [{}]", input, e);
			throw new IllegalArgumentException(e);
		}
	}
}
