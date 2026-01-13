/*
   Copyright 2024-2026 WeAreFrank!

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.frankframework.console.ApiException;

/**
 * This ExceptionHandler catches {@link ApiException ApiExceptions} thrown by the Frank!Framework.
 * The exceptions are similar to an ErrorResponseException as they contain HTTP headers and a body.
 */
@RestControllerAdvice("org.frankframework.console")
@Order(Ordered.HIGHEST_PRECEDENCE+1000)
public class ApiExceptionHandler {

	@ExceptionHandler(ApiException.class)
	protected ResponseEntity<Object> handleApiException(ApiException exception) {
		return exception.getResponse();
	}
}
