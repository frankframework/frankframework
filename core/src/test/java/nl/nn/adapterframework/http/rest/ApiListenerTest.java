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
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.ArrayList;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.http.rest.ApiListener.AuthenticationMethods;

import org.junit.Before;
import org.junit.Test;

public class ApiListenerTest {

	private ApiListener listener;

	@Before
	public void setUp() {
		listener = new ApiListener();
		listener.setName("my-api-listener");
		listener.setMethod("put");
		listener.setUriPattern("dummy");
	}

	@Test
	public void testProducesLowerCase() throws ConfigurationException {
		listener.setProduces("xml");
		listener.configure();

		assertEquals("XML", listener.getProduces());
	}

	@Test
	public void testProducesUpperCase() throws ConfigurationException {
		listener.setProduces("XML");
		listener.configure();

		assertEquals("XML", listener.getProduces());
	}

	@Test
	public void testProducesTextWithCharset() throws ConfigurationException {
		listener.setProduces("TEXT");
		listener.setCharacterEncoding("utf-8");
		listener.configure();

		assertEquals("TEXT", listener.getProduces());
		assertEquals("text/plain;charset=UTF-8", listener.getContentType());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUnknownProduces() throws ConfigurationException {
		listener.setProduces("unknown");
	}

	@Test
	public void testConsumesLowerCase() throws ConfigurationException {
		listener.setConsumes("xml");
		listener.configure();

		assertEquals("XML", listener.getConsumes());
	}

	@Test
	public void testConsumesUpperCase() throws ConfigurationException {
		listener.setConsumes("XML");
		listener.configure();

		assertEquals("XML", listener.getConsumes());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testUnknownConsumes() throws ConfigurationException {
		listener.setConsumes("unknown");
	}

	@Test
	public void testContentTypes() throws ConfigurationException {
		for(MediaTypes type : MediaTypes.values()) {
			listener.setProduces(type.name());
			listener.configure(); //Check if the mediatype passes the configure checks

			assertTrue(listener.getContentType().startsWith(type.getContentType()));
		}

		//Check empty produces
		listener.setProduces("");
		listener.configure();

		assertEquals("*/*", listener.getContentType());
	}

	@Test
	public void isConsumableXML() throws ConfigurationException {
		String acceptHeader = "text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8";

		listener.setConsumes("XML");
		assertTrue("can parse [XML]", listener.isConsumable(acceptHeader));

		listener.setConsumes("JSON");
		assertFalse("can parse [JSON]", listener.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableJSON() throws ConfigurationException {
		String acceptHeader = "text/html, application/json;q=0.9, */*;q=0.8";

		listener.setConsumes("XML");
		assertFalse("can parse [XML]", listener.isConsumable(acceptHeader));

		listener.setConsumes("JSON");
		assertTrue("can parse [JSON]", listener.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableANY() throws ConfigurationException {
		String acceptHeader = "application/octet-stream";

		listener.setConsumes("ANY");
		assertTrue("can parse anything", listener.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableMULTIPARTS() throws ConfigurationException {
		//There are different multipart contentTypes, see: https://msdn.microsoft.com/en-us/library/ms527355(v=exchg.10).aspx
		//Test at least the 3 most commonly used multiparts
		List<String> acceptHeaders = new ArrayList<String>();
		acceptHeaders.add("multipart/form-data");
		acceptHeaders.add("multipart/related");
		acceptHeaders.add("multipart/mixed");

		listener.setConsumes("MULTIPART");
		for(String header : acceptHeaders) {
			String acceptHeader = header + "; type=text/html; q=0.7, "+header+"; level=2; q=0.4; boundary=--my-top-notch-boundary-";

			assertTrue("can parse ["+header+"]", listener.isConsumable(acceptHeader));
		}
	}

	@Test
	public void listenerAcceptsAll() throws ConfigurationException {
		String contentType = "application/octet-stream";
		String acceptHeader = contentType + "; type=text/html; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces("ANY");
		assertTrue("accepts anything", listener.accepts(acceptHeader));
	}

	@Test
	public void clientAcceptsAll() throws ConfigurationException {
		String contentType = "application/xhtml+xml, application/xml";
		String acceptHeader = contentType + "; type=text/html; q=0.7, */*; level=2; q=0.4";

		listener.setProduces("JSON");
		assertTrue("accepts anything", listener.accepts(acceptHeader));
	}

	@Test
	public void doesNotAcceptOctetStreamWhenJSON() throws ConfigurationException {
		String contentType = "application/octet-stream";
		String acceptHeader = contentType + "; type=text/html; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces("JSON");
		assertFalse("does not accept an octet-stream when set to JSON", listener.accepts(acceptHeader));
	}

	@Test
	public void acceptsJson() throws ConfigurationException {
		String contentType = "application/json";
		String acceptHeader = contentType + "; type=text/html; q=0.7, "+contentType+"; level=2; q=0.4";

		listener.setProduces("JSON");
		assertTrue("accepts JSON", listener.accepts(acceptHeader));
	}

	@Test(expected = ConfigurationException.class)
	public void testFaultyAuthMethod() throws ConfigurationException {
		try{
			listener.setAuthenticationMethod("unknown$df");
		}
		finally {
			assertEquals("No authentication method should be set", AuthenticationMethods.NONE, listener.getAuthenticationMethod());
		}
	}

	@Test
	public void testAuthRoleMethod() throws ConfigurationException {
		listener.setAuthenticationMethod(AuthenticationMethods.AUTHROLE.name());
		assertEquals("Authentication method [AUTHROLE] should be set", AuthenticationMethods.AUTHROLE, listener.getAuthenticationMethod());
	}
}
