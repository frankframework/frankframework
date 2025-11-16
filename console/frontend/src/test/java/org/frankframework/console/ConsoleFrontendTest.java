package org.frankframework.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.UnsupportedEncodingException;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

public class ConsoleFrontendTest {
	private MockServletContext context = new MockServletContext();
	private MockServletConfig config = new MockServletConfig(context);
	private ConsoleFrontend servlet;

	@BeforeEach
	public void setup() throws ServletException {
		servlet = new ConsoleFrontend();
		servlet.afterPropertiesSet();
		servlet.init(config);
	}

	private MockHttpServletRequest createRequest(String path) {
		MockHttpServletRequest request = new MockHttpServletRequest(context, "GET", path == null ? "" : path);
		request.setPathInfo(path);
		return request;
	}

	@Test
	public void findFileThatDoesNotExist() throws UnsupportedEncodingException {
		MockHttpServletRequest request = createRequest("/doesnt-exist.txt");

		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doGet(request, response);

		assertEquals(404, response.getStatus());
		assertEquals("", response.getContentAsString());
	}

	@Test
	public void testFileRetrieval() throws UnsupportedEncodingException {
		MockHttpServletRequest request = createRequest("/dummy.txt");

		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doGet(request, response);

		assertEquals(200, response.getStatus());
		assertEquals("console/test", response.getContentAsString());
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void noPathShouldRedirect(String path) throws UnsupportedEncodingException {
		MockHttpServletRequest request = createRequest(path);

		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doGet(request, response);

		assertEquals(302, response.getStatus());
		assertEquals("", response.getContentAsString());
	}

	@ParameterizedTest
	@ValueSource(strings = {"/", "/index.html", "/#/../index.html"})
	public void testIndex(String path) throws UnsupportedEncodingException {
		MockHttpServletRequest request = createRequest(path);

		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doGet(request, response);

		assertEquals(200, response.getStatus());
		assertEquals("<html />", response.getContentAsString());
	}

	@ParameterizedTest
	@ValueSource(strings = {"http://other.url/bar/foo", "/localhost:1337/foo/bar", "without-slash",
			"../dummy.txt", "/../dummy.txt", "/%20.txt", "/#/../../dummy.txt"})
	// Tests  ForbiddenUrls
	// Tests FileRetrieval outside 'ConsoleDirectory'
	public void testForbinnenAndRelativeUrls(String path) throws UnsupportedEncodingException {
		MockHttpServletRequest request = createRequest(path);

		MockHttpServletResponse response = new MockHttpServletResponse();
		servlet.doGet(request, response);

		assertEquals(404, response.getStatus());
		assertEquals("", response.getContentAsString());
	}
}
