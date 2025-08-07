/*
   Copyright 2024 WeAreFrank!

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
package org.frankframework.console.configuration;

import jakarta.annotation.Nullable;
import jakarta.servlet.ServletException;

import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.method.MethodValidationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.console.ApiException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE+1)
@Log4j2
public class ApiExceptionHandler {

	/** Taken from {@link ResponseEntityExceptionHandler#handleException(Exception, WebRequest)} } */
	@ExceptionHandler({
			HttpRequestMethodNotSupportedException.class,
			HttpMediaTypeNotSupportedException.class,
			HttpMediaTypeNotAcceptableException.class,
			MissingPathVariableException.class,
			MethodArgumentNotValidException.class,
			NoResourceFoundException.class,
			AsyncRequestTimeoutException.class,
			ErrorResponseException.class,
			MaxUploadSizeExceededException.class,
			Exception.class
	})
	protected final ResponseEntity<Object> handleException(Exception ex) {
		if (ex instanceof ErrorResponse errorResponseEx) {
			return handleSpringException(ex, errorResponseEx.getStatusCode(), errorResponseEx.getHeaders());
		}
		return handleExceptionLowLevel(ex);
	}

	/** Lower level exceptions */
	@ExceptionHandler({
			MissingServletRequestParameterException.class,
			MissingServletRequestPartException.class,
			ServletRequestBindingException.class,
			ConversionNotSupportedException.class,
			TypeMismatchException.class,
			HttpMessageNotReadableException.class,
			HttpMessageNotWritableException.class,
			MethodValidationException.class,
			BindException.class
	})
	protected final ResponseEntity<Object> handleExceptionLowLevel(Exception ex) {
		HttpHeaders headers = new HttpHeaders();
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		if (ex instanceof ServletException
				|| ex instanceof TypeMismatchException
				|| ex instanceof HttpMessageNotReadableException
				|| ex instanceof BindException
		) {
			status = HttpStatus.BAD_REQUEST;
		}
		return handleSpringException(ex, status, headers);
	}

	/** When a {@link RestController} cannot be found, Spring MVC throws this Exception. */
	@ExceptionHandler(NoHandlerFoundException.class)
	protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex) {
		return ApiException.formatExceptionResponse(ex.getMessage(), HttpStatus.NOT_FOUND);
	}

	@ExceptionHandler(ApiException.class)
	protected ResponseEntity<Object> handleApiException(ApiException exception) {
		return exception.getResponse();
	}

	protected ResponseEntity<Object> handleSpringException(Exception exception, HttpStatusCode status, @Nullable HttpHeaders headers) {
		log.warn("Caught unhandled exception while executing FF!API call", exception);
		return ApiException.formatExceptionResponse(exception.getMessage(), status, headers);
	}
}
