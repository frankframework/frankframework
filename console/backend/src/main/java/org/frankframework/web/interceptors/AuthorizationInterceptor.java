package org.frankframework.web.interceptors;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.frankframework.management.bus.BusMessageUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.lang.reflect.Method;

//@Priority(Priorities.AUTHORIZATION)
// TODO figure out how to do this in spring security (or if it even belongs there)
public class AuthorizationInterceptor implements HandlerInterceptor {

	private static final int FORBIDDEN = HttpStatus.FORBIDDEN.value();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
		if("OPTIONS".equalsIgnoreCase(request.getMethod())) {
			//Preflight in here should not be possible?
			return true;
		}

		HandlerMethod handlerMethod = (HandlerMethod) handler;
		Method method = handlerMethod.getMethod();

		if(method.isAnnotationPresent(DenyAll.class)) {
			//Functionality has been disallowed.
			response.sendError(FORBIDDEN);
			return false;
		}

		//Presume `PermitAll` when RolesAllowed annotation is not set
		if(method.isAnnotationPresent(RolesAllowed.class)) {
			RolesAllowed rolesAnnotation = method.getAnnotation(RolesAllowed.class);
			if(!BusMessageUtils.hasAnyRole(rolesAnnotation.value())) {
				response.sendError(FORBIDDEN);
				return false;
			}
		}

		return true;
	}

}
