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
package org.frankframework.console.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.lang.reflect.Method;

/**
 * Should only intercept Spring WEB MVC requests.
 * If a method has the {@link Deprecated} annotation it should be omitted (by default).
 */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DeprecationInterceptor implements HandlerInterceptor, EnvironmentAware {

	public static final String ALLOW_DEPRECATED_ENDPOINTS_KEY = "iaf-api.allowDeprecated";

	private final Logger log = LogManager.getLogger(this);
	private boolean allowDeprecatedEndpoints = false;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		if(handler instanceof HandlerMethod handlerMethod) {
			Method method = handlerMethod.getMethod();

			if(!allowDeprecatedEndpoints && method.isAnnotationPresent(Deprecated.class)) {
				log.warn("endpoint [{}] has been deprecated, set property [{}=true] to restore functionality", request.getRequestURI(), ALLOW_DEPRECATED_ENDPOINTS_KEY);
				response.sendError(HttpStatus.BAD_REQUEST.value());
				return false;
			}
		}
		return true;
	}

	@Override
	public void setEnvironment(Environment environment) {
		allowDeprecatedEndpoints = environment.getProperty(ALLOW_DEPRECATED_ENDPOINTS_KEY, boolean.class, false);
	}
}
