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
package nl.nn.adapterframework.http.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.http.rest.ApiListener.AuthenticationMethods;
import nl.nn.adapterframework.http.rest.ApiListener.HttpMethod;

import org.junit.Before;
import org.junit.Test;

public class ApiListenerTest {

	private ApiListener listener;

	@Before
	public void setUp() {
		listener = new ApiListener();
		listener.setName("my-api-listener");
		listener.setMethod(HttpMethod.PUT);
		listener.setUriPattern("dummy");
	}

	@Test
	public void testOptionsMethod() throws ConfigurationException {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
			listener.setMethod(HttpMethod.OPTIONS);
		});
		assertEquals("unknown httpMethod value [optIonS]. Must be one of [GET, PUT, POST, PATCH, DELETE]", ex.getMessage());
	}


	@Test
	public void testProducesTextWithCharset() throws ConfigurationException {
		listener.setProduces(MediaTypes.TEXT);
		listener.setCharacterEncoding("utf-8");
		listener.configure();

		assertEquals(MediaTypes.TEXT, listener.getProduces());
		assertEquals("text/plain;charset=UTF-8", listener.getContentType().getContentType());
	}


	@Test
	public void testContentTypes() throws ConfigurationException {
		for(MediaTypes type : MediaTypes.values()) {
			listener.setProduces(type);
			listener.configure(); //Check if the mediatype passes the configure checks

			assertTrue(listener.getContentType().getContentType().startsWith(type.getContentType()));
		}
	}

	@Test
	public void testEmptyContentTypes() throws ConfigurationException {
		//Check empty produces
		listener.setProduces(null);
		listener.setConsumes(null);
		listener.configure();

		assertEquals("*/*", listener.getContentType().getContentType());
		assertEquals(MediaTypes.ANY, listener.getConsumes());
	}

	@Test
	public void isConsumableXML() {
		String acceptHeader = "text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8";

		listener.setConsumes(MediaTypes.XML);
		assertTrue("can parse [XML]", listener.isConsumable(acceptHeader));

		listener.setConsumes(MediaTypes.JSON);
		assertFalse("can parse [JSON]", listener.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableJSON() {
		String acceptHeader = "text/html, application/json;q=0.9, */*;q=0.8";

		listener.setConsumes(MediaTypes.XML);
		assertFalse("can parse [XML]", listener.isConsumable(acceptHeader));

		listener.setConsumes(MediaTypes.JSON);
		assertTrue("can parse [JSON]", listener.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableANY() {
		String acceptHeader = "application/octet-stream";

		listener.setConsumes(MediaTypes.ANY);
		assertTrue("can parse anything", listener.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableMULTIPARTS() {
		//There are different multipart contentTypes, see: https://msdn.microsoft.com/en-us/library/ms527355(v=exchg.10).aspx
		//Test at least the 3 most commonly used multiparts
		List<String> acceptHeaders = new ArrayList<String>();
		acceptHeaders.add("multipart/form-data");
		acceptHeaders.add("multipart/related");
		acceptHeaders.add("multipart/mixed");

		listener.setConsumes(MediaTypes.MULTIPART);
		for(String header : acceptHeaders) {
			String acceptHeader = header + "; type=text/html; q=0.7, "+header+"; level=2; q=0.4; boundary=--my-top-notch-boundary-";

			assertTrue("can parse ["+header+"]", listener.isConsumable(acceptHeader));
		}
	}

	@Test
	public void listenerAcceptsAll() {
		String contentType = "application/octet-stream";
		String acceptHeader = contentType + "; type=text/html; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces(MediaTypes.ANY);
		assertTrue("accepts anything", listener.accepts(acceptHeader));
	}

	@Test
	public void clientAcceptsAll() {
		String contentType = "application/xhtml+xml, application/xml";
		String acceptHeader = contentType + "; type=text/html; q=0.7, */*; level=2; q=0.4";

		listener.setProduces(MediaTypes.JSON);
		assertTrue("accepts anything", listener.accepts(acceptHeader));
	}

	@Test
	public void doesNotAcceptOctetStreamWhenJSON() {
		String contentType = "application/octet-stream";
		String acceptHeader = contentType + "; type=text/html; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces(MediaTypes.JSON);
		assertFalse("does not accept an octet-stream when set to JSON", listener.accepts(acceptHeader));
	}

	@Test
	public void acceptsJson() {
		String contentType = "application/json";
		String acceptHeader = contentType + "; type=text/html; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces(MediaTypes.JSON);
		assertTrue("accepts JSON", listener.accepts(acceptHeader));
	}

	@Test
	public void testAuthRoleMethod() {
		listener.setAuthenticationMethod(AuthenticationMethods.AUTHROLE);
		assertEquals("Authentication method [AUTHROLE] should be set", AuthenticationMethods.AUTHROLE, listener.getAuthenticationMethod());
	}
}
