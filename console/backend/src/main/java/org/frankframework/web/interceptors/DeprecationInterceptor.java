package org.frankframework.web.interceptors;

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

/* does work for some reason */
@Order(Ordered.LOWEST_PRECEDENCE)
public class DeprecationInterceptor implements HandlerInterceptor, EnvironmentAware {

	public static final String ALLOW_DEPRECATED_ENDPOINTS_KEY = "iaf-api.allowDeprecated";

	private final Logger log = LogManager.getLogger(this);
	private boolean allowDeprecatedEndpoints = false;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
		log.error("preHandle - {}", handler);
		HandlerMethod handlerMethod = (HandlerMethod) handler;
		Method method = handlerMethod.getMethod();

		if(!allowDeprecatedEndpoints && method.isAnnotationPresent(Deprecated.class)) {
			log.warn("endpoint [{}] has been deprecated, set property [{}=true] to restore functionality", request.getRequestURI(), ALLOW_DEPRECATED_ENDPOINTS_KEY);
			response.sendError(HttpStatus.BAD_REQUEST.value());
			return false;
		}

		return true;
	}

	@Override
	public void setEnvironment(Environment environment) {
		allowDeprecatedEndpoints = environment.getProperty(ALLOW_DEPRECATED_ENDPOINTS_KEY, boolean.class, false);
	}
}
