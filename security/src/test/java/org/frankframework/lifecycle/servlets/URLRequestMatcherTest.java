package org.frankframework.lifecycle.servlets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

public class URLRequestMatcherTest {

	@Test
	public void simpleTest() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/servlet/*");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlet/testpath");
		request.setServletPath("/servlet/");
		request.setPathInfo("testpath");
		assertTrue(matcher.matches(request));
	}

	@Test
	public void multipleSlashesInServletPath() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/ser/vlet/path/*");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ser/vlet/path/testpath");
		request.setServletPath("/ser/vlet/path/");
		request.setPathInfo("testpath");
		assertTrue(matcher.matches(request));
	}

	@Test
	public void dontMatchTheWrongEndpoint() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/servlet/*");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlet2/testpath");
		request.setServletPath("/servlet2/");
		request.setPathInfo("testpath");
		assertFalse(matcher.matches(request));
	}

	@Test
	public void dontMatchWhenNotWildcard() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/servlet/");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlet/testpath");
		request.setServletPath("/servlet");
		request.setPathInfo("/testpath");
		assertFalse(matcher.matches(request));
	}

	@Test
	public void dontMatchWhenPathDoesNotEndWithSlash() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/servlet");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlet/");
		request.setServletPath("/servlet");
		request.setPathInfo("/");
		assertFalse(matcher.matches(request));
	}

	@Test
	public void dontMatchWhenPathEndsWithSlash() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/servlet/");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlet");
		request.setServletPath("/servlet");
		request.setPathInfo(null);
		assertFalse(matcher.matches(request));
	}

	@Test
	public void noEndpoints() {
		Set<String> endpoints = new HashSet<>();
		assertThrows(IllegalArgumentException.class, () -> new URLRequestMatcher(endpoints));
	}

	@Test
	public void absoluteMatchWithSlash() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/servlet/");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/servlet/");
		request.setServletPath("/servlet/");
		request.setPathInfo(null);
		assertTrue(matcher.matches(request));
	}

	@Test
	public void noValidEndpointsProvided() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("!/servlet");
		assertThrows(IllegalArgumentException.class, () -> new URLRequestMatcher(endpoints));
	}

	@Test
	public void rootPath() {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/*");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", "/foo/bar");
		request.setServletPath("/");
		request.setPathInfo("foo/bar");
		assertTrue(matcher.matches(request));
	}

	@ParameterizedTest
	@ValueSource(strings = { "/api", "/api/" })
	public void absolutePathEndingWithAsterisk(String path) {
		Set<String> endpoints = new HashSet<>();
		endpoints.add("/api/*");
		URLRequestMatcher matcher = new URLRequestMatcher(endpoints);

		MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
		request.setServletPath("/");
		request.setPathInfo("api");
		assertTrue(matcher.matches(request));

		request.setPathInfo("api/");
		assertTrue(matcher.matches(request));

		request.setPathInfo("monkey/");
		assertFalse(matcher.matches(request));
	}
}
