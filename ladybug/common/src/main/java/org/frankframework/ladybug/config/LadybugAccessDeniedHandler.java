/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.ladybug.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerExceptionResolver;

@RestControllerAdvice({"org.frankframework.ladybug", "org.wearefrank.ladybug"})
public class LadybugAccessDeniedHandler implements AccessDeniedHandler {
	private static final Logger APPLICATION_LOG = LogManager.getLogger("APPLICATION");
	private HandlerExceptionResolver resolver;

	public LadybugAccessDeniedHandler() {
		APPLICATION_LOG.error("Constructing LadybugAccessDeniedHandler, will inject HandlerExceptionResolver later to prevent circular dependencies");
	}

	public void setHandlerExceptionResolver(HandlerExceptionResolver handlerExceptionResolver) {
		APPLICATION_LOG.error("Setting HandlerExceptionResolver");
		this.resolver = handlerExceptionResolver;
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
