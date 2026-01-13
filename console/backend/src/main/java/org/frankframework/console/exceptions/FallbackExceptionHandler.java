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

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.frankframework.console.ApiException;

/**
 * This ExceptionHandler catches all remaining {@link Exception exceptions} and formats them.
 * We only handle raw Exceptions that are thrown from our own packages.
 */
@RestControllerAdvice("org.frankframework.console")
public class FallbackExceptionHandler {

	@ExceptionHandler(Exception.class)
	protected final ResponseEntity<Object> handleException(Exception ex) {
		return ApiException.formatExceptionResponse(ex.getMessage(), HttpStatusCode.valueOf(500));
	}

}
