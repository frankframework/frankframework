package org.frankframework.console.controllers;

import jakarta.servlet.ServletException;

import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.ApiException;

/**
 * Test controller to replicate throwing of Exceptions in controller methods to test the ApiExceptionHandler
 */
@RestController
public class TestController {

	@GetMapping(value = "/test/apiexception", produces = MediaType.APPLICATION_JSON_VALUE)
	public String testApiException() {
		throw new ApiException("Er gaat iets mis");
	}

	@GetMapping(value = "/test/servletexception", produces = MediaType.APPLICATION_JSON_VALUE)
	public String testServletException() throws ServletException {
		throw new ServletException("Er gaat iets mis");
	}

	@GetMapping(value = "/test/methodnotsupportedexception", produces = MediaType.APPLICATION_JSON_VALUE)
	public String testBindException() throws HttpRequestMethodNotSupportedException {
		throw new HttpRequestMethodNotSupportedException("param");
	}
}
