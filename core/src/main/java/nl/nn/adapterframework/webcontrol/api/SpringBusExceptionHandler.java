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

import nl.nn.adapterframework.management.bus.BusException;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Custom errorHandler for the FF!API to catch all unhandled exceptions.
 * 
 * @author	Niels Meijer
 */

@Provider
public class SpringBusExceptionHandler implements ExceptionMapper<MessageHandlingException> {

	private Logger log = LogUtil.getLogger(this);

	@Override
	public Response toResponse(MessageHandlingException mhe) {
		Throwable cause = mhe.getCause();
		while(cause != null && !(cause instanceof BusException)) {
			cause = cause.getCause();
		}

		if(cause != null) { //Found a BusException, throw it directly
			return ApiException.formatException(cause.getMessage(), Status.INTERNAL_SERVER_ERROR);
		}

		log.warn("unhandled exception while sending/receiving information from the Application Bus", mhe);
		return ApiException.formatException(mhe.getMessage(), Status.INTERNAL_SERVER_ERROR);
	}
}
