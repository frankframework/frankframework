package org.frankframework.console.controllers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.servlet.ServletException;

import org.springframework.http.MediaType;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.ApiException;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@interface DescriptionAuditTestController {
}

/**
 * Test controller to replicate throwing of Exceptions in controller methods to test the ApiExceptionHandler
 */
@DescriptionAuditTestController
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
