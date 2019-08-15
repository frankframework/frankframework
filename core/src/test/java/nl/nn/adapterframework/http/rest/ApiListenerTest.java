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

import org.junit.Before;
import org.junit.Test;

public class ApiListenerTest {

	private ApiListener listener;

	@Before
	public void setUp() {
		listener = new ApiListener();
		listener.setName("my-api-listener");
		listener.setMethod("put");
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
		for(MediaType type : MediaType.values()) {
			listener.setProduces(type.name());
			listener.configure(); //Check if the mediatype passes the configure checks

			assertEquals(type.getContentType(), listener.getContentType());
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
}
