/*
   Copyright 2016-2022 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Custom errorHandler for the FF!API to unpack and re-pack {@link WebApplicationException}s.
 * Has to be explicitly configured to override the CXF default {@link WebApplicationException}Listener.
 * 
 * @author	Niels Meijer
 */

@Provider
public class ApiExceptionHandler implements ExceptionMapper<WebApplicationException> {

	private Logger log = LogManager.getLogger(this);

	@Override
	public Response toResponse(WebApplicationException exception) {
		//If the message has already been wrapped in an exception we don't need to `convert` it!
		if(exception instanceof ApiException) {
			return ((ApiException) exception).getResponse();
		}

		log.warn("Caught unhandled WebApplicationException while executing FF!API call", exception);

		Status status = Status.INTERNAL_SERVER_ERROR;
		Response response = exception.getResponse();
		if(response != null && response.getStatus() > 0) {
			status = Status.fromStatusCode(response.getStatus());
		}
		return ApiException.formatExceptionResponse(exception.getMessage(), status);
	}
}
