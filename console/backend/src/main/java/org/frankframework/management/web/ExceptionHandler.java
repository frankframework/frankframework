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
package org.frankframework.management.web;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Custom errorHandler for the FF!API to catch all unhandled exceptions.
 *
 * @author	Niels Meijer
 */

@Provider
public class ExceptionHandler implements ExceptionMapper<Exception> {

	private Logger log = LogManager.getLogger(this);

	@Override
	public Response toResponse(Exception exception) {
		log.warn("Caught unhandled Exception while executing FF!API call", exception);

		return ApiException.formatExceptionResponse(exception.getMessage(), Status.INTERNAL_SERVER_ERROR);
	}
}
