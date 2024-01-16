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
package org.frankframework.management.web;

import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonStructure;
import jakarta.json.JsonWriter;
import jakarta.json.JsonWriterFactory;
import jakarta.json.stream.JsonGenerator;

import org.frankframework.core.IbisException;

/**
 * Custom errors for the API.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

public class ApiException extends WebApplicationException implements Serializable {

	private static final long serialVersionUID = 2L;
	private final transient Logger log = LogManager.getLogger(this);
	private final Status status;
	private final String expandedMessage;
	private transient Response response;

	public ApiException(String msg) {
		this(msg, Status.INTERNAL_SERVER_ERROR);
	}

	public ApiException(Throwable t) {
		this(t, 500);
	}

	public ApiException(String msg, Throwable t) {
		this(msg, t, null);
	}

	private ApiException(Throwable t, int status) {
		this(null, t, Status.fromStatusCode(status));
	}

	public ApiException(String msg, int status) {
		this(msg, Status.fromStatusCode(status));
	}

	public ApiException(String msg, Status status) {
		this(msg, null, status);
	}

	private ApiException(String msg, Throwable t, Status status) {
		super(msg, t);

		this.status = (status!=null) ? status: Status.INTERNAL_SERVER_ERROR;
		if(msg == null && t == null) {
			this.expandedMessage = null;
		} else {
			this.expandedMessage = IbisException.expandMessage(super.getMessage(), this, e -> e instanceof IbisException || e instanceof ApiException);
		}

		log.warn(this.expandedMessage, t);
	}

	@Override
	public String getMessage() {
		return expandedMessage;
	}

	@Override
	public Response getResponse() {
		if(response == null) {
			response = formatExceptionResponse(expandedMessage, status);
		}
		return response;
	}

	protected static Response formatExceptionResponse(String message, Status status) {
		ResponseBuilder builder = Response.status(status).type(MediaType.TEXT_PLAIN);

		if(message != null) {
			JsonObjectBuilder json = Json.createObjectBuilder();
			json.add("status", status.getReasonPhrase());
			//Replace non ASCII characters, tabs, spaces and newlines.
			json.add("error", message.replace("\n", " ").replace(System.getProperty("line.separator"), " "));

			builder.type(MediaType.APPLICATION_JSON);
			builder.entity(new FormattedJsonEntity(json.build()));
		}

		return builder.build();
	}

	static class FormattedJsonEntity implements StreamingOutput {
		private final JsonWriterFactory factory;
		private final JsonStructure json;

		public FormattedJsonEntity(JsonStructure json) {
			Map<String, Boolean> config = new HashMap<>();
			config.put(JsonGenerator.PRETTY_PRINTING, true);
			factory = Json.createWriterFactory(config);
			this.json = json;
		}

		@Override
		public void write(OutputStream output) {
			try (JsonWriter jsonWriter = factory.createWriter(output, StandardCharsets.UTF_8)) {
				jsonWriter.write(json);
			}
		}
	}
}
