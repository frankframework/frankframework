package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CustomizedForwardedHeaderFilterTest {

	private CustomizedForwardedHeaderFilter filter;
	private MockHttpServletRequest request;
	private MockHttpServletResponse response;
	private FilterChain filterChain;

	@BeforeEach
	void setup() {
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
		filterChain = mock();

		request.setRemoteAddr("127.0.0.1");
		request.setRemoteHost("localhost");
		request.addHeader("X-My-Header", "My-Header-Value");
		request.addHeader("X-Forwarded-Host", "example.org");
		request.addHeader("X-Forwarded-For", "example.com");
	}

	@Test
	void testFilterAllowAllHeaders() throws ServletException, IOException {
		// Arrange
		filter = new CustomizedForwardedHeaderFilter(true);
		request.addHeader("X-Forwarded-Proto", "https");

		ArgumentCaptor<HttpServletRequest> requestArgumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		ArgumentCaptor<HttpServletResponse> responseArgumentCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);

		doNothing().when(filterChain).doFilter(requestArgumentCaptor.capture(), responseArgumentCaptor.capture());

		// Act
		filter.doFilter(request, response, filterChain);

		// Assert
		HttpServletRequest capturedRequest = requestArgumentCaptor.getValue();

		Set<String> headerNames = convertEnumerationToSet(capturedRequest.getHeaderNames());

		assertTrue(headerNames.contains("X-Forwarded-Host"));
		assertTrue(headerNames.contains("X-My-Header"));

		assertEquals("example.com", capturedRequest.getRemoteHost());
		assertEquals("example.org", capturedRequest.getServerName());
		assertTrue(capturedRequest.isSecure());
	}

	@Test
	void testFilterSuppressForwardedHeaders() throws ServletException, IOException {
		// Arrange
		filter = new CustomizedForwardedHeaderFilter(false);
		request.addHeader("X-Forwarded-Proto", "http");

		ArgumentCaptor<HttpServletRequest> requestArgumentCaptor = ArgumentCaptor.forClass(HttpServletRequest.class);
		ArgumentCaptor<HttpServletResponse> responseArgumentCaptor = ArgumentCaptor.forClass(HttpServletResponse.class);

		doNothing().when(filterChain).doFilter(requestArgumentCaptor.capture(), responseArgumentCaptor.capture());

		// Act
		filter.doFilter(request, response, filterChain);

		// Assert
		HttpServletRequest capturedRequest = requestArgumentCaptor.getValue();

		Set<String> headerNames = convertEnumerationToSet(capturedRequest.getHeaderNames());

		assertFalse(headerNames.contains("X-Forwarded-Host"));
		assertTrue(headerNames.contains("X-My-Header"));

		assertEquals("example.com", capturedRequest.getRemoteHost());
		assertEquals("example.org", capturedRequest.getServerName());
		assertFalse(capturedRequest.isSecure());
	}

	private static <E> Set<E> convertEnumerationToSet(Enumeration<E> enumeration) {
		return new HashSet<>(Collections.list(enumeration));
	}
}
