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

import java.util.function.Function;

import jakarta.annotation.Nullable;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.console.ApiException;
import org.frankframework.management.bus.BusException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@Log4j2
public class SpringBusExceptionHandler {

	public enum ManagedException {
		AUTHENTICATION(AuthenticationException.class, HttpStatus.UNAUTHORIZED), // Authentication exception
		AUTHORIZATION(AccessDeniedException.class, HttpStatus.FORBIDDEN), // Authorization exception
		BUS_EXCEPTION(BusException.class, SpringBusExceptionHandler.ManagedException::convertBusException), // Managed BUS exception
		BACKEND_UNAVAILABLE(ResourceAccessException.class, HttpStatus.SERVICE_UNAVAILABLE), // Server doesn't exist
		REQUEST_EXCEPTION(HttpClientErrorException.class, SpringBusExceptionHandler.ManagedException::convertHttpClientError); // Server refused connection

		private final Class<? extends Throwable> exceptionClass;
		private final Function<Throwable, ResponseEntity<?>> messageConverter;

		ManagedException(final Class<? extends Throwable> exceptionClass, final HttpStatus status) {
			this(exceptionClass, e -> ApiException.formatExceptionResponse(e.getMessage(), status));
		}

		@SuppressWarnings("unchecked")
		<T extends Throwable> ManagedException(final Class<T> exceptionClass, final Function<T, ResponseEntity<?>> messageConverter) {
			this.exceptionClass = exceptionClass;
			this.messageConverter = (Function<Throwable, ResponseEntity<?>>) messageConverter;
		}

		public ResponseEntity<?> toResponse(Throwable cause) {
			return messageConverter.apply(cause);
		}

		public static @Nullable ManagedException parse(Throwable cause) {
			for (ManagedException me : ManagedException.values()) {
				if (me.exceptionClass.isAssignableFrom(cause.getClass())) {
					return me;
				}
			}
			return null;
		}

		/**
		 * Returns the StatusCode + reason phrase for the given status code.
		 */
		private static ResponseEntity<?> convertHttpClientError(HttpClientErrorException e) {
			HttpStatusCode status = e.getStatusCode();
			if (status.is5xxServerError() || status == HttpStatus.NOT_FOUND) {
				String reasonPhrase = HttpStatus.valueOf(status.value()).getReasonPhrase();
				return ApiException.formatExceptionResponse(status.value() + " - " + reasonPhrase, status);
			}
			return ApiException.formatExceptionResponse(e.getResponseBodyAsString(), status);
		}

		/**
		 * Returns the StatusCode + reason phrase for the given status code.
		 */
		private static ResponseEntity<?> convertBusException(BusException e) {
			HttpStatus status = HttpStatus.valueOf(e.getStatusCode());
			return ApiException.formatExceptionResponse(e.getMessage(), status);
		}
	}

	@ExceptionHandler(BusException.class)
	public ResponseEntity<?> toResponse(BusException be) {
		HttpStatus status = HttpStatus.valueOf(be.getStatusCode());
		return ApiException.formatExceptionResponse(be.getMessage(), status);
	}

	@ExceptionHandler(MessageHandlingException.class)
	public ResponseEntity<?> toResponse(MessageHandlingException mhe) {
		Throwable cause = mhe.getCause();
		for (int i = 0; i < 5 && cause != null; i++) {
			ManagedException mex = ManagedException.parse(cause);
			if (mex != null) { // If a ManagedException is found, throw it directly
				return mex.toResponse(cause);
			}
			cause = cause.getCause();
		}

		log.warn("unhandled exception while sending/receiving information from the Application Bus", mhe);
		return ApiException.formatExceptionResponse(buildMessage(mhe.getMessage(), mhe.getCause()), HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private static String buildMessage(@Nullable String message, @Nullable Throwable cause) {
		if (cause == null) {
			return message;
		}
		StringBuilder sb = new StringBuilder(64);
		if (message != null) {
			sb.append(message).append("; ");
		}
		sb.append("nested exception is ").append(cause);
		return sb.toString();
	}

}
