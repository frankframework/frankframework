package org.frankframework.management.web.spring;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.frankframework.core.IbisException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class ApiException extends RuntimeException implements Serializable {

	private static final long serialVersionUID = 2L;
	private final transient Logger log = LogManager.getLogger(this);
	private final HttpStatus status;
	private final String expandedMessage;
	private transient ResponseEntity<Object> response;

	public ApiException(String msg) {
		this(msg, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	public ApiException(Throwable t) {
		this(t, 500);
	}

	public ApiException(String msg, Throwable t) {
		this(msg, t, null);
	}

	private ApiException(Throwable t, int status) {
		this(null, t, HttpStatus.valueOf(status));
	}

	public ApiException(String msg, int status) {
		this(msg, HttpStatus.valueOf(status));
	}

	public ApiException(String msg, HttpStatus status) {
		this(msg, null, status);
	}

	private ApiException(String msg, Throwable t, HttpStatus status) {
		super(msg, t);
		this.status = status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR;
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

	public ResponseEntity<Object> getResponse() {
		if(response == null) {
			response = formatExceptionResponse(expandedMessage, status, null);
		}
		return response;
	}

	protected static ResponseEntity<Object> formatExceptionResponse(String message, HttpStatus status, @Nullable HttpHeaders headers) {
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(status).contentType(MediaType.TEXT_PLAIN);

		if(headers != null) {
			builder.headers(headers);
		}

		if(message != null) {
			Map<String,String> json = new HashMap<>();
			json.put("status", status.getReasonPhrase());
			//Replace non ASCII characters, tabs, spaces and newlines.
			json.put("error", message.replace("\n", " ").replace(System.lineSeparator(), " "));

			builder.contentType(MediaType.APPLICATION_JSON);
			return builder.body(json);
		}

		return builder.build();
	}

}
