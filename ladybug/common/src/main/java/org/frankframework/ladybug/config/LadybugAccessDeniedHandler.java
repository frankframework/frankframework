package org.frankframework.ladybug.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerExceptionResolver;

@RestControllerAdvice({"org.frankframework.ladybug", "org.wearefrank.ladybug"})
public class LadybugAccessDeniedHandler implements AccessDeniedHandler {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");
	private final HandlerExceptionResolver resolver;

	public LadybugAccessDeniedHandler(HandlerExceptionResolver resolver) {
		if (resolver == null) {
			APPLICATION_LOG.error("Creating LadybugAccessDeniedHandler with null HandlerExceptionResolver");
		} else {
			APPLICATION_LOG.error("Creating LadybugAccessDeniedHandler with non-null HandlerExceptionResolver");
		}
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
