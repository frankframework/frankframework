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
package org.frankframework.console.exceptions;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.frankframework.console.ApiException;
import org.frankframework.console.exceptions.SpringBusExceptionHandler.ManagedException;

/**
 * Authentication and authorization exceptions thrown by the console controllers themselves, for instance by method security
 * ({@code @RolesAllowed} / {@code @AllowAllIbisUserRoles}) when the user holds none of the required roles.
 * Without this handler they end up in the {@link FallbackExceptionHandler} and are answered with a 500.
 * <p>
 * This handler is deliberately limited to the console's own controllers. Other modules that run in the same
 * application context (such as the Ladybug) have their own handling for these exceptions, with their own response format.
 * </p>
 */
@RestControllerAdvice("org.frankframework.console")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConsoleSecurityExceptionHandler {

	@ExceptionHandler({ AuthenticationException.class, AccessDeniedException.class })
	public ResponseEntity<?> toResponse(RuntimeException e) {
		ManagedException mex = ManagedException.parse(e);
		return mex != null ? mex.toResponse(e) : ApiException.formatExceptionResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
