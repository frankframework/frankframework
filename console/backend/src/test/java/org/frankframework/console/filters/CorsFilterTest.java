package org.frankframework.console.filters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Properties;

import org.frankframework.util.SpringUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

public class CorsFilterTest {
	public static final String STUBBED_SPRING_BUS_CONFIGURATION = "stubbedBusApplicationContext.xml";

	private CorsFilter createFilter(Properties properties) throws Exception {
		ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext();
		applicationContext.setConfigLocation(STUBBED_SPRING_BUS_CONFIGURATION);
		applicationContext.setDisplayName("CorsFilterTest-ApplicationContext");

		MutablePropertySources propertySources = applicationContext.getEnvironment().getPropertySources();
		propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
		propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
		propertySources.addFirst(new PropertiesPropertySource("testConfig", properties));

		applicationContext.refresh();

		CorsFilter filter = SpringUtils.createBean(applicationContext, CorsFilter.class);
		filter.init(null);
		return filter;
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testCorsDisabled(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "false");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest(method, "/dummy");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(0, response.getHeaderNames().size(), "no cors origin, so no response headers");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testCorsFilterWithoutOrigin(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "true");
		properties.setProperty("cors.origin", "https://domain.com:2345");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest(method, "/dummy");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(0, response.getHeaderNames().size(), "no cors origin, so no response headers");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testCorsFilterWithOrigin(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "true");
		properties.setProperty("cors.origin", "https://domain.com:2345");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest(method, "/dummy");
		request.addHeader("Origin", "https://tornado.shrimp.com");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(0)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(0, response.getHeaderNames().size(), "no valid cors, so request aborted");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testCorsFilterWithMultipleAllowedOrigins(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "true");
		properties.setProperty("cors.origin", "https://domain.com:2345,http://torpedo.schrim.com,https://foo.bar/");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest(method, "/dummy");
		request.addHeader("Origin", "http://torpedo.schrim.com");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(4, response.getHeaderNames().size(), "cors passed, so cors headers should be set");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testCorsWildcardOrigins(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "true");
		properties.setProperty("cors.origin", "https://domain.com:2345,http://*.schrim.com");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest(method, "/dummy");
		request.addHeader("Origin", "http://torpedo.schrim.com");
		request.addHeader("Access-Control-Request-Headers", "Allow");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(5, response.getHeaderNames().size(), "cors passed, so cors headers should be set");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testCorsWildcardOriginsAndPort(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "true");
		properties.setProperty("cors.origin", "http://*.schrim.com:2345");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest(method, "/dummy");
		request.addHeader("Origin", "http://torpedo.schrim.com:2345");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(4, response.getHeaderNames().size(), "cors passed, so cors headers should be set");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testCorsAllOrigins(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "true");
		properties.setProperty("cors.origin", "*");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest(method, "/dummy");
		request.addHeader("Origin", "http://torpedo.schrim.com:2345");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(4, response.getHeaderNames().size(), "cors passed, so cors headers should be set");
	}

	@ParameterizedTest
	@ValueSource(strings = {"GET", "PUT", "POST"})
	public void testOptionsRequest(String method) throws Exception {
		// Arrange
		Properties properties = new Properties();
		properties.setProperty("cors.enforced", "true");
		properties.setProperty("cors.origin", "*");
		CorsFilter filter = createFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/dummy");
		request.addHeader("Origin", "http://torpedo.schrim.com:2345");
		request.addHeader("Access-Control-Request-Method", method);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = spy(MockFilterChain.class);

		// Act
		filter.doFilter(request, response, chain);

		// Assert
		verify(chain, times(0)).doFilter(any(ServletRequest.class), any(ServletResponse.class));
		assertEquals(5, response.getHeaderNames().size(), "cors passed, but options request, so no headers");
	}
}
