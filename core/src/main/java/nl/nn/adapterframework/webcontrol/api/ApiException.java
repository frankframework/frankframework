/*
Copyright 2016-2017 Integration Partners B.V.

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

import java.io.Serializable;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import nl.nn.adapterframework.util.LogUtil;

import org.apache.logging.log4j.Logger;

/**
 * Custom errors for the API.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

public class ApiException extends WebApplicationException implements Serializable
{
	private static final long serialVersionUID = 1L;
	private Logger log = LogUtil.getLogger(this);

	public ApiException() {
		super();
	}

	public ApiException(Throwable t) {
		this(t, 500);
	}

	public ApiException(String msg, Throwable t) {
		this(msg, t, Status.INTERNAL_SERVER_ERROR);
	}

	public ApiException(Throwable t, int status) {
		this(t.getMessage(), t, Status.fromStatusCode(status));
	}

	private ApiException(String msg, Throwable t, Status status) {
		super(msg, t, formatException(msg, status, MediaType.APPLICATION_JSON));

		log.error(msg, t);
	}



	public ApiException(String msg) {
		this(msg, Status.INTERNAL_SERVER_ERROR);
	}

	public ApiException(String msg, int status) {
		this(msg, Status.fromStatusCode(status));
	}

	public ApiException(String msg, Status status) {
		super(msg, formatException(msg, status, MediaType.APPLICATION_JSON));
	}

	private static Response formatException(String message, Status status, String mediaType) {
		ResponseBuilder response = Response.status(status).type(mediaType);

		if(message != null) {
			message = message.replace("\"", "\\\"").replace("\n", " ").replace(System.getProperty("line.separator"), " ");

			response.entity(("{\"status\":\""+status.getReasonPhrase()+"\", \"error\":\"" + message + "\"}"));
		}
		return response.build();
	}
}
