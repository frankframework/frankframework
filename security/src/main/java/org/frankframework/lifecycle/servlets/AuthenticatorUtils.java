/*
   Copyright 2024 WeAreFrank!

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

import java.lang.reflect.Method;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import lombok.extern.log4j.Log4j2;

import org.frankframework.util.ClassUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;

@Log4j2
public class AuthenticatorUtils {


	/**
	 * Disable login on endpoints when using local setup.
	 *
	 * @param environment set of properties used to determine the dtap stage.
	 * @return the default `AuthenticationType` name.
	 */
	private static String getDefaultAuthenticationType() {
		AuthenticationType defaultAuthenticator = SecuritySettings.isWebSecurityEnabled() ? AuthenticationType.SEALED : AuthenticationType.NONE;
		return defaultAuthenticator.name();
	}

	public static IAuthenticator createAuthenticator(ApplicationContext applicationContext, String properyPrefix) {
		Environment environment = applicationContext.getEnvironment();
		String type = environment.getProperty(properyPrefix + "type", getDefaultAuthenticationType());
		AuthenticationType auth = null;
		try {
			auth = EnumUtils.parse(AuthenticationType.class, type);
		} catch (IllegalArgumentException e) {
			throw new IllegalStateException("invalid authenticator type", e);
		}
		log.debug("creating Authenticator [{}]", auth);
		Class<? extends IAuthenticator> clazz = auth.getAuthenticator();
		IAuthenticator authenticator = SpringUtils.createBean(applicationContext, clazz);

		for (Method method : clazz.getMethods()) {
			if (!method.getName().startsWith("set") || method.getParameterTypes().length != 1)
				continue;

			String setter = StringUtil.lcFirst(method.getName().substring(3));
			String value = environment.getProperty(properyPrefix + setter);
			if (StringUtils.isNotEmpty(value)) {
				ClassUtils.invokeSetter(authenticator, method, value);
			}
		}

		return authenticator;
	}

	/**
	 * A request is treated as an API (non-browser) request — and therefore challenged with a 401
	 * instead of a login redirect — when it carries a bearer token or does not accept HTML.
	 */
	static boolean isApiRequest(HttpServletRequest request) {
		return hasAuthorizationBearerHeader(request) || !hasAcceptHtmlHeader(request);
	}

	/**
	 * @return whether the request has an "Accept: text/html" header, indicating a browser request
	 */
	private static boolean hasAcceptHtmlHeader(HttpServletRequest request) {
		String acceptHeader = request.getHeader(HttpHeaders.ACCEPT);

		return StringUtils.isNotEmpty(acceptHeader)
				&& acceptHeader.contains(MediaType.TEXT_HTML_VALUE);
	}

	/**
	 * @return whether an "Authorization: Bearer" header is present
	 */
	static boolean hasAuthorizationBearerHeader(HttpServletRequest request) {
		String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

		return StringUtils.isNotEmpty(authorizationHeader)
				&& authorizationHeader.toLowerCase().startsWith("bearer ");
	}
}
