package org.frankframework.ladybug.config;

import jakarta.servlet.http.HttpServletRequest;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class LadybugAccessDeniedHandler implements AccessDeniedHandler {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");
	private final HandlerExceptionResolver resolver;

	public LadybugAccessDeniedHandler(HandlerExceptionResolver resolver) {
		this.resolver = resolver;
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException ex
	) {
		APPLICATION_LOG.error("LadybugAccessDeniedHandler was triggered for exception", ex);
		resolver.resolveException(request, response, null, ex);
	}
}
