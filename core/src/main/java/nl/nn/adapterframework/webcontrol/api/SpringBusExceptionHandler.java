/*
Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;

import lombok.Getter;
import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Catch Spring Channel exceptions. Some Exceptions may be thrown direct, see {@link ManagedException}.
 * 
 * @author	Niels Meijer
 */

@Provider
public class SpringBusExceptionHandler implements ExceptionMapper<MessageHandlingException> {

	private Logger log = LogUtil.getLogger(this);

	public enum ManagedException {
		AUTHENTICATION(Status.UNAUTHORIZED, AuthenticationCredentialsNotFoundException.class),
		AUTHORIZATION(Status.FORBIDDEN, AccessDeniedException.class),
		BUS_EXCEPTION(Status.INTERNAL_SERVER_ERROR, BusException.class);

		private final @Getter Status status;
		private final Class<? extends Exception> exceptionClass;

		private ManagedException(Status status, Class<? extends Exception> exceptionClass) {
			this.status = status;
			this.exceptionClass = exceptionClass;
		}

		public static ManagedException parse(Throwable cause) {
			for(ManagedException me : ManagedException.values()) {
				if(cause.getClass().isAssignableFrom(me.exceptionClass)) {
					return me;
				}
			}
			return null;
		}
	}

	@Override
	public Response toResponse(MessageHandlingException mhe) {
		Throwable cause = mhe.getCause();
		for(int i = 0; i < 5 && cause != null; i++) {
			ManagedException mex = ManagedException.parse(cause);
			if(mex != null) { //If a ManagedException is found, throw it directly
				return ApiException.formatExceptionResponse(cause.getMessage(), mex.getStatus());
			}
			cause = cause.getCause();
		}

		log.warn("unhandled exception while sending/receiving information from the Application Bus", mhe);
		return ApiException.formatExceptionResponse(mhe.getMessage(), Status.INTERNAL_SERVER_ERROR);
	}
}
