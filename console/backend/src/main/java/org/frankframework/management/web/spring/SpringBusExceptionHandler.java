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
package org.frankframework.management.web.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.management.bus.BusException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import java.util.function.Function;

@RestControllerAdvice
public class SpringBusExceptionHandler {

	private Logger log = LogManager.getLogger(this);

	public enum ManagedException {
		AUTHENTICATION(AuthenticationException.class, HttpStatus.UNAUTHORIZED), //Authentication exception
		AUTHORIZATION(AccessDeniedException.class, HttpStatus.FORBIDDEN), // Authorization exception
		BUS_EXCEPTION(BusException.class, SpringBusExceptionHandler.ManagedException::convertBusException), // Managed BUS exception
		BACKEND_UNAVAILABLE(ResourceAccessException.class, HttpStatus.SERVICE_UNAVAILABLE), // Server doesn't exist
		REQUEST_EXCEPTION(HttpClientErrorException.class, SpringBusExceptionHandler.ManagedException::convertHttpClientError); // Server refused connection

		private final Class<? extends Throwable> exceptionClass;
		private final Function<Throwable, ResponseEntity<?>> messageConverter;

		private ManagedException(final Class<? extends Throwable> exceptionClass, final HttpStatus status) {
			this(exceptionClass, e -> ApiException.formatExceptionResponse(e.getMessage(), status, null));
		}

		@SuppressWarnings("unchecked")
		private <T extends Throwable> ManagedException(final Class<T> exceptionClass, final Function<T, ResponseEntity<?>> messageConverter) {
			this.exceptionClass = exceptionClass;
			this.messageConverter = (Function<Throwable, ResponseEntity<?>>) messageConverter;
		}

		public ResponseEntity<?> toResponse(Throwable cause) {
			return messageConverter.apply(cause);
		}

		public static SpringBusExceptionHandler.ManagedException parse(Throwable cause) {
			for(SpringBusExceptionHandler.ManagedException me : SpringBusExceptionHandler.ManagedException.values()) {
				if(me.exceptionClass.isAssignableFrom(cause.getClass())) {
					return me;
				}
			}
			return null;
		}

		/**
		 * Returns the StatusCode + reason phrase for the given status code.
		 */
		private static ResponseEntity<?> convertHttpClientError(HttpClientErrorException e) {
			HttpStatus status = HttpStatus.valueOf(e.getRawStatusCode());
			if(status.is5xxServerError() || status == HttpStatus.NOT_FOUND) {
				return ApiException.formatExceptionResponse(status.value() + " - " + status.getReasonPhrase(), status, null);
			}
			return ApiException.formatExceptionResponse(e.getResponseBodyAsString(), status, null);
		}

		/**
		 * Returns the StatusCode + reason phrase for the given status code.
		 */
		private static ResponseEntity<?> convertBusException(BusException e) {
			HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
			return ApiException.formatExceptionResponse(e.getMessage(), status, null);
		}
	}


	@ExceptionHandler({
			MessageHandlingException.class
	})
	public ResponseEntity<?> toResponse(MessageHandlingException mhe) {
		Throwable cause = mhe.getCause();
		for(int i = 0; i < 5 && cause != null; i++) {
			SpringBusExceptionHandler.ManagedException mex = SpringBusExceptionHandler.ManagedException.parse(cause);
			if(mex != null) { //If a ManagedException is found, throw it directly
				return mex.toResponse(cause);
			}
			cause = cause.getCause();
		}

		log.warn("unhandled exception while sending/receiving information from the Application Bus", mhe);
		return ApiException.formatExceptionResponse(mhe.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, null);
	}

}
