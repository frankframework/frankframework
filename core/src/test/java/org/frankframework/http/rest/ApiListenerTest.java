/*
Copyright 2019 Integration Partners B.V.

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
package org.frankframework.http.rest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.http.rest.ApiListener.AuthenticationMethods;
import org.frankframework.http.rest.ApiListener.HttpMethod;
import org.frankframework.lifecycle.DynamicRegistration.Servlet;
import org.frankframework.lifecycle.ServletManager;
import org.frankframework.lifecycle.servlets.ServletConfiguration;

public class ApiListenerTest {

	private ApiListener listener;

	@BeforeEach
	public void setUp() {
		listener = new ApiListener();
		listener.setName("my-api-listener");
		listener.setMethod(HttpMethod.PUT);
		listener.setUriPattern("dummy");
	}

	@Test
	public void testOptionsMethod() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> listener.setMethod(HttpMethod.OPTIONS));
		assertEquals("method OPTIONS should not be added manually", ex.getMessage());
	}


	@Test
	public void testProducesTextWithCharset() throws ConfigurationException {
		listener.setProduces(MediaTypes.TEXT);
		listener.setCharacterEncoding("utf-8");
		listener.configure();

		assertEquals(MediaTypes.TEXT, listener.getProduces());
		assertEquals("text/plain;charset=UTF-8", listener.getContentType().toString());
	}


	@Test
	public void testContentTypes() throws ConfigurationException {
		for(MediaTypes type : MediaTypes.values()) {
			listener.setProduces(type);
			listener.configure(); //Check if the media-type passes the 'configure' checks

			assertTrue(listener.getContentType().includes(type.getMimeType()));
		}
	}

	@Test
	public void testEmptyContentTypes() throws ConfigurationException {
		//Check empty produces
//		listener.setProduces(null);
//		listener.setConsumes(null);
		listener.configure();

		assertEquals("*/*", listener.getContentType().toString());
		assertEquals(MediaTypes.ANY, listener.getConsumes());
	}

	@Test
	public void isConsumableXML() {
		String contentType = "application/xml;charset=UTF8;level=2";

		listener.setConsumes(MediaTypes.XML);
		assertTrue(listener.isConsumable(contentType), "can parse [XML]");

		listener.setConsumes(MediaTypes.JSON);
		assertFalse(listener.isConsumable(contentType), "can parse [JSON]");
	}

	@Test
	public void isConsumableJSON() {
		String contentType = "application/json;charset=UTF8;level=2";

		listener.setConsumes(MediaTypes.XML);
		assertFalse(listener.isConsumable(contentType), "can parse [XML]");

		listener.setConsumes(MediaTypes.JSON);
		assertTrue(listener.isConsumable(contentType), "can parse [JSON]");
	}

	@Test
	public void isConsumableANY() {
		String acceptHeader = "application/octet-stream";

		listener.setConsumes(MediaTypes.ANY);
		assertTrue(listener.isConsumable(acceptHeader), "can parse anything");
	}

	@Test
	public void isConsumableEmptyContentTypeHeader() {
		// Arrange
		listener.setConsumes(MediaTypes.ANY);

		// Act / Assert
		assertTrue(listener.isConsumable(null), "listener should be able to parse anything");
		assertTrue(listener.isConsumable(""), "listener should be able to parse anything");

		// Arrange
		listener.setConsumes(MediaTypes.XML);

		// Act / Assert
		assertFalse(listener.isConsumable(null), "listener should be able to parse XML");
		assertFalse(listener.isConsumable(""), "listener should be able to parse XML");

		// Arrange
		listener.setConsumes(MediaTypes.JSON);

		// Act / Assert
		assertFalse(listener.isConsumable(null), "listener should be able to parse JSON");
		assertFalse(listener.isConsumable(""), "listener should be able to parse JSON");
	}

	@Test
	public void isConsumableMULTIPARTS() {
		//There are different multipart contentTypes, see: https://msdn.microsoft.com/en-us/library/ms527355(v=exchg.10).aspx
		//Test at least the 3 most commonly used multiparts
		List<String> acceptHeaders = new ArrayList<>();
		acceptHeaders.add("multipart/form-data");
		acceptHeaders.add("multipart/related");
		acceptHeaders.add("multipart/mixed");

		listener.setConsumes(MediaTypes.MULTIPART);
		for(String header : acceptHeaders) {
			String acceptHeader = header + "; type=text; "+header+"; level=2; boundary=--my-top-notch-boundary-";

			assertTrue(listener.isConsumable(acceptHeader), "can parse ["+header+"]");
		}
	}

	@Test
	public void listenerAcceptsAll() {
		String contentType = "application/octet-stream";
		String acceptHeader = contentType + "; type=text/html; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces(MediaTypes.ANY);
		assertTrue(listener.accepts(acceptHeader), "accepts anything");
	}

	@Test
	public void clientAcceptsAll() {
		String acceptHeader = "application/xhtml+xml, application/xml; type=text; q=0.7, */*; level=2; q=0.4";

		listener.setProduces(MediaTypes.JSON);
		assertTrue(listener.accepts(acceptHeader), "accepts anything");
	}

	@Test
	public void clientInvalidAcceptHeader() {
		String contentType = "application/xhtml+xml, application/xml";
		String acceptHeader = contentType + "; type=text/html; q=0.7, level=2; q=0.4";

		listener.setProduces(MediaTypes.JSON);
		assertFalse(listener.accepts(acceptHeader), "does not accept invalid Accept header");
	}

	@Test
	public void doesNotAcceptOctetStreamWhenJSON() {
		String contentType = "application/octet-stream";
		String acceptHeader = contentType + "; type=text; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces(MediaTypes.JSON);
		assertFalse(listener.accepts(acceptHeader), "does not accept an octet-stream when set to JSON");
	}

	@Test
	public void acceptsJson() {
		String contentType = "application/json";
		String acceptHeader = contentType + "; type=text; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces(MediaTypes.JSON);
		assertTrue(listener.accepts(acceptHeader), "listener should be able to accept JSON");
	}

	@Test
	public void testAuthRoleMethod() {
		listener.setAuthenticationMethod(AuthenticationMethods.AUTHROLE);
		assertEquals(AuthenticationMethods.AUTHROLE, listener.getAuthenticationMethod(), "Authentication method [AUTHROLE] should be set");
	}

	@Test
	public void testGetPhysicalDestinationName() throws Exception {
		listener.configure();
		assertEquals("uriPattern: /dummy; method: PUT", listener.getPhysicalDestinationName());
	}

	@Test
	public void testGetPhysicalDestinationNameConsumesProduces() throws Exception {
		listener.setConsumes(MediaTypes.JSON);
		listener.setProduces(MediaTypes.XML);
		listener.configure();
		assertEquals("uriPattern: /dummy; method: PUT; consumes: JSON; produces: XML", listener.getPhysicalDestinationName());
	}

	@Test
	public void testGetPhysicalDestinationNameWith1Endpoint() throws Exception {
		ServletManager manager = spy(new ServletManager());
		Servlet servlet = mock(Servlet.class);
		when(servlet.getName()).thenReturn(ApiListenerServlet.class.getSimpleName());
		when(servlet.getUrlMapping()).thenReturn("aapje/*");
		ServletConfiguration servletConfig = spy(new ServletConfiguration());
		doNothing().when(servletConfig).loadProperties();
		servletConfig.fromServlet(servlet);
		when(manager.getServlet(anyString())).thenReturn(servletConfig);
		listener.setServletManager(manager);

		listener.setConsumes(MediaTypes.JSON);
		listener.setProduces(MediaTypes.XML);
		listener.configure();
		assertEquals("uriPattern: /aapje/dummy; method: PUT; consumes: JSON; produces: XML", listener.getPhysicalDestinationName());
	}

	@Test
	public void testGetPhysicalDestinationNameWith2Endpoints() throws Exception {
		ServletManager manager = spy(new ServletManager());
		Servlet servlet = mock(Servlet.class);
		when(servlet.getName()).thenReturn(ApiListenerServlet.class.getSimpleName());
		when(servlet.getUrlMapping()).thenReturn("aap/*,/noot/*");
		ServletConfiguration servletConfig = spy(new ServletConfiguration());
		doNothing().when(servletConfig).loadProperties();
		servletConfig.fromServlet(servlet);
		when(manager.getServlet(anyString())).thenReturn(servletConfig);
		listener.setServletManager(manager);

		listener.setConsumes(MediaTypes.JSON);
		listener.setProduces(MediaTypes.XML);
		listener.configure();
		assertEquals("uriPattern: [/aap, /noot]/dummy; method: PUT; consumes: JSON; produces: XML", listener.getPhysicalDestinationName());
	}

	@Test
	public void testValidatingMissingUriPattern() {
		// Given
		listener.setUriPattern("");

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "uriPattern cannot be empty");
	}

	@ParameterizedTest
	@CsvSource({
			"/good/pat/*, true",
			"/good/pat/**, true",
			"/good/pat/*/friet, true",
			"/bad/pat/a*, false",
			"/bad/pat/a**, false",
			"/bad/pat/***, false",
			"/bad/pat/a*/friet, false",
			"/bad/pat/*t, false",
			"/bad/pat/*t/friet, false",
			"/bad/pat/**/friet, false",
			"/bad/pat/**/friet/**, false",
	})
	public void testUriPatternValidation(String uriPattern, boolean expected) {
		// Act
		boolean isValid = ApiListener.isValidUriPattern(uriPattern);

		// Assert
		assertEquals(expected, isValid, "URI Pattern [" + uriPattern + "] should have been " + (expected ? "" : "in")
				+ "valid but was matched as " + (isValid ? "" : "in") + "valid.");
	}

	@Test
	public void testValidatingMissingJwksURL() {
		// Given
		listener.setAuthenticationMethod(AuthenticationMethods.JWT);

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "jwksUrl cannot be empty");
	}

	@Test
	public void testCannotConsumeGET(){
		// Given
		listener.setMethod(HttpMethod.GET);
		listener.setConsumes(MediaTypes.JSON);

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "cannot set consumes attribute when using method [GET]");
	}

	@Test
	public void testCannotConsumeGETMultiMethod(){
		// Given
		listener.setMethods(HttpMethod.GET, HttpMethod.POST);
		listener.setConsumes(MediaTypes.JSON);

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "cannot set consumes attribute when using method [GET]");
	}

	@Test
	public void testCannotConsumeDELETE(){
		// Given
		listener.setMethod(HttpMethod.DELETE);
		listener.setConsumes(MediaTypes.JSON);

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "cannot set consumes attribute when using method [DELETE]");
	}

	@Test
	public void testCannotConsumeDELETEMultiMethod(){
		// Given
		listener.setMethods(HttpMethod.DELETE, HttpMethod.POST);
		listener.setConsumes(MediaTypes.JSON);

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "cannot set consumes attribute when using method [DELETE]");
	}

	@Test
	public void testValidatingAnyMatchClaims() {
		// Given
		listener.setAuthenticationMethod(AuthenticationMethods.JWT);
		listener.setJwksURL("dummy");
		listener.setAnyMatchClaims("claim=value,claim2=value2");

		// Expect/When
		assertDoesNotThrow(listener::configure);
	}

	@Test
	public void testValidatingAnyMatchClaimsInvalid() {
		// Given
		listener.setAuthenticationMethod(AuthenticationMethods.JWT);
		listener.setJwksURL("dummy");
		listener.setAnyMatchClaims("claim=value,claim2,claim3=value=too_long");

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "[claim2,claim3=value=too_long] are not valid key/value pairs for [anyMatchClaims].");
	}

	@Test
	public void testValidatingAnyMatchClaimsOneInvalid() {
		// Given
		listener.setAuthenticationMethod(AuthenticationMethods.JWT);
		listener.setJwksURL("dummy");
		listener.setAnyMatchClaims("claim2");

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "[claim2] is not a valid key/value pair for [anyMatchClaims].");
	}

	@Test
	public void testValidatingExactMatchClaims() {
		// Given
		listener.setAuthenticationMethod(AuthenticationMethods.JWT);
		listener.setJwksURL("dummy");
		listener.setExactMatchClaims("claim=value,claim2=value2");

		// Expect/When
		assertDoesNotThrow(listener::configure);
	}

	@Test
	public void testValidatingExactMatchClaimsInvalid() {
		// Given
		listener.setAuthenticationMethod(AuthenticationMethods.JWT);
		listener.setJwksURL("dummy");
		listener.setExactMatchClaims("claim=value,claim2,claim3=value=too_long");

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "[claim2,claim3=value=too_long] are not valid key/value pairs for [exactMatchClaims].");
	}

	@Test
	public void testValidatingExactMatchClaimsOneInvalid() {
		// Given
		listener.setAuthenticationMethod(AuthenticationMethods.JWT);
		listener.setJwksURL("dummy");
		listener.setExactMatchClaims("claim2");

		// Expect/When
		assertThrows(ConfigurationException.class, listener::configure, "[claim2] is not a valid key/value pair for [exactMatchClaims].");
	}
}
