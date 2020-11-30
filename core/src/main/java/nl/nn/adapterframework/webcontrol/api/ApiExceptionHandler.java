/*
Copyright 2016-2017, 2020 WeAreFrank!

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

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.util.LogUtil;

/**
 * Register custom errorHandler for the API.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Provider
public class ApiExceptionHandler implements ExceptionMapper<Exception> {

	private Logger log = LogUtil.getLogger(this);

	@Override
	@Produces(MediaType.TEXT_PLAIN)
	public Response toResponse(Exception exception) {
		//If the message has already been wrapped in an exception we don't need to `convert` it!
		if(exception instanceof ApiException) {
			return ((ApiException) exception).getResponse();
		}

		log.error("Caught exception in handling FF!API call", exception);

		ResponseBuilder response = Response.status(Status.INTERNAL_SERVER_ERROR);
		String message = exception.getMessage();

		if(message != null) {
			message = message.replace("\"", "\\\"").replace("\n", " ").replace(System.getProperty("line.separator"), " ");
			Map<String, Object> entity = new HashMap<String, Object>(3);
			entity.put("status", "error");
			entity.put("error", message);
			entity.put("stackTrace", exception.getStackTrace());

			response.entity(entity).type(MediaType.APPLICATION_JSON);
		}

		return response.build();
	}
}