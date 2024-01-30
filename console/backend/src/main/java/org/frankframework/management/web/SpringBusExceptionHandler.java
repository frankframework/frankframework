/*
   Copyright 2022-2023 WeAreFrank!

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
package org.frankframework.management.web;

import java.util.function.Function;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import org.frankframework.management.bus.BusException;

/**
 * Catch Spring Channel exceptions. Some Exceptions may be thrown direct, see {@link ManagedException}.
 *
 * @author	Niels Meijer
 */

@Provider
public class SpringBusExceptionHandler implements ExceptionMapper<MessageHandlingException> {

	private Logger log = LogManager.getLogger(this);

	public enum ManagedException {
		AUTHENTICATION(AuthenticationException.class, Status.UNAUTHORIZED), //Authentication exception
		AUTHORIZATION(AccessDeniedException.class, Status.FORBIDDEN), // Authorization exception
		BUS_EXCEPTION(BusException.class, Status.BAD_REQUEST), // Managed BUS exception
		BACKEND_UNAVAILABLE(ResourceAccessException.class, Status.SERVICE_UNAVAILABLE), // Server doesn't exist
		REQUEST_EXCEPTION(HttpClientErrorException.class, ManagedException::convertHttpClientError); // Server refused connection

		private final Class<? extends Throwable> exceptionClass;
		private final Function<Throwable, Response> messageConverter;

		private ManagedException(final Class<? extends Throwable> exceptionClass, final Status status) {
			this(exceptionClass, e -> ApiException.formatExceptionResponse(e.getMessage(), status));
		}

		@SuppressWarnings("unchecked")
		private <T extends Throwable> ManagedException(final Class<T> exceptionClass, final Function<T, Response> messageConverter) {
			this.exceptionClass = exceptionClass;
			this.messageConverter = (Function<Throwable, Response>) messageConverter;
		}

		public Response toResponse(Throwable cause) {
			return messageConverter.apply(cause);
		}

		public static ManagedException parse(Throwable cause) {
			for(ManagedException me : ManagedException.values()) {
				if(me.exceptionClass.isAssignableFrom(cause.getClass())) {
					return me;
				}
			}
			return null;
		}

		/**
		 * Returns the StatusCode + reason phrase for the given status code.
		 */
		private static Response convertHttpClientError(HttpClientErrorException e) {
			Status status = Status.fromStatusCode(e.getRawStatusCode());
			if(Family.SERVER_ERROR == status.getFamily() || status == Status.NOT_FOUND) {
				return ApiException.formatExceptionResponse(status.getStatusCode() + " - " + status.getReasonPhrase(), status);
			}
			return ApiException.formatExceptionResponse(e.getResponseBodyAsString(), status);
		}
	}

	@Override
	public Response toResponse(MessageHandlingException mhe) {
		Throwable cause = mhe.getCause();
		for(int i = 0; i < 5 && cause != null; i++) {
			ManagedException mex = ManagedException.parse(cause);
			if(mex != null) { //If a ManagedException is found, throw it directly
				return mex.toResponse(cause);
			}
			cause = cause.getCause();
		}

		log.warn("unhandled exception while sending/receiving information from the Application Bus", mhe);
		return ApiException.formatExceptionResponse(mhe.getMessage(), Status.INTERNAL_SERVER_ERROR);
	}
}
