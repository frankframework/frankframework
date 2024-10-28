package org.frankframework.lifecycle.servlets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.env.Environment;

public class ServletConfigurationTest {

	private ServletConfiguration createServletConfiguration() {
		ServletConfiguration config = new ServletConfiguration();
		Environment environment = mock(Environment.class);
		when(environment.getProperty(anyString())).thenReturn("CONTAINER");
		config.setEnvironment(environment);
		config.afterPropertiesSet();
		return config;
	}

	@ParameterizedTest
	@ValueSource(strings = {"test", "/test", "/test,  "})
	public void testSingleUrl(String url) {
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping(url);

		assertEquals(1, config.getUrlMapping().size());
		assertThat(config.getUrlMapping(), hasItem("/test"));
	}

	@Test
	public void testMultilineUrl() {
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/one/*, /two,three/,   */four/,*five");

		assertEquals(5, config.getUrlMapping().size());
		assertThat(config.getUrlMapping(), hasItem("/one/*"));
		assertThat(config.getUrlMapping(), hasItem("/two"));
		assertThat(config.getUrlMapping(), hasItem("/three/"));
		assertThat(config.getUrlMapping(), hasItem("*/four/"));
		assertThat(config.getUrlMapping(), hasItem("*five"));
	}

	@ParameterizedTest
	@ValueSource(strings = {"", ",   ", "   "})
	public void testEmptyUrls(String endpointSet) {
		ServletConfiguration config = createServletConfiguration();
		assertThrows(IllegalStateException.class, ()->config.setUrlMapping(endpointSet));
	}

	@Test
	public void testFaultyExclude() {
		ServletConfiguration config = createServletConfiguration();
		assertThrows(IllegalStateException.class, ()->config.setUrlMapping("/one/*,!one/healthcheck"));
	}

	@Test
	public void testFaultyExcludeWildcard() {
		ServletConfiguration config = createServletConfiguration();
		assertThrows(IllegalStateException.class, ()->config.setUrlMapping("/one/*,!/one/healthcheck/*"));
	}

	@Test
	public void testExclude() {
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/one/*,!/one/healthcheck");

		assertEquals(2, config.getUrlMapping().size());
		assertThat(config.getUrlMapping(), hasItem("/one/*"));
		assertThat(config.getUrlMapping(), hasItem("!/one/healthcheck"));
	}

	@Test
	public void testRootPath() {
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("/*");

		assertEquals(1, config.getUrlMapping().size());
		assertThat(config.getUrlMapping(), hasItem("/*"));
	}

	@Test
	public void testWildcard() {
		ServletConfiguration config = createServletConfiguration();
		config.setUrlMapping("*");

		assertEquals(1, config.getUrlMapping().size());
		assertThat(config.getUrlMapping(), hasItem("/*"));
	}
}
