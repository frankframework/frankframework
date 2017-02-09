/*
Copyright 2016 Integration Partners B.V.

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

/**
* Custom errors for the API.
* 
* @author	Niels Meijer
*/

public class ApiException extends WebApplicationException implements Serializable
{
	private static final long serialVersionUID = 1L;

	public ApiException() {
		super();
	}

	public ApiException(Exception e) {
		super(e);
	}

	public ApiException(String msg) {
		super(formatException(msg, Status.INTERNAL_SERVER_ERROR, MediaType.APPLICATION_JSON));
	}

	public ApiException(String msg, int status) {
		super(formatException(msg, Status.fromStatusCode(status), MediaType.APPLICATION_JSON));
	}

	public ApiException(String msg, Status status) {
		super(formatException(msg, status, MediaType.APPLICATION_JSON));
	}

	public ApiException(String msg, int status, String MediaType) {
		super(formatException(msg, Status.fromStatusCode(status), MediaType));
	}

	public ApiException(String msg, Status status, String MediaType) {
		super(formatException(msg, status, MediaType));
	}

	private static Response formatException(String message, Status status, String mediaType) {
		ResponseBuilder response = Response.status(status).type(mediaType);

		if(message != null) {
			message = message.replace("\"", "\\\"").replace("\n", " ").replace(System.getProperty("line.separator"), " ");

			response.entity(("{\"status\":\"error\", \"error\":\"" + message + "\"}"));
		}
		return response.build();
	}
}
