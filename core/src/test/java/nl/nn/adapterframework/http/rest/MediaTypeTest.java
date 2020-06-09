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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class MediaTypeTest {

	@Test
	public void fromValue() {
		//Test the 3 most commonly used mediaTypes
		assertEquals("fromValue [XML]", MediaTypes.XML, MediaTypes.fromValue("application/xml"));
		assertEquals("fromValue [JSON]", MediaTypes.JSON, MediaTypes.fromValue("application/json"));
		assertEquals("fromValue [TEXT]", MediaTypes.TEXT, MediaTypes.fromValue("text/plain"));
	}

	@Test(expected=IllegalArgumentException.class)
	public void fromValueUnknown() {
		MediaTypes.fromValue("something/unknown");
	}

	@Test
	public void isConsumableXML() {
		String acceptHeader = "text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8";

		assertTrue("can parse [XML]", MediaTypes.XML.isConsumable(acceptHeader));

		assertFalse("can parse [JSON]", MediaTypes.JSON.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableJSON() {
		String acceptHeader = "text/html, application/json;q=0.9, */*;q=0.8";

		assertFalse("can parse [XML]", MediaTypes.XML.isConsumable(acceptHeader));

		assertTrue("can parse [JSON]", MediaTypes.JSON.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableANY() {
		String acceptHeader = "application/octet-stream";

		assertTrue("can parse anything", MediaTypes.ANY.isConsumable(acceptHeader));
	}

	@Test
	public void isConsumableMULTIPARTS() {
		//There are different multipart contentTypes, see: https://msdn.microsoft.com/en-us/library/ms527355(v=exchg.10).aspx
		//Test at least the 3 most commonly used multiparts
		List<String> acceptHeaders = new ArrayList<String>();
		acceptHeaders.add("multipart/form-data");
		acceptHeaders.add("multipart/related");
		acceptHeaders.add("multipart/mixed");

		for(String header : acceptHeaders) {
			String acceptHeader = header + "; type=text/html; q=0.7, "+header+"; level=2; q=0.4; boundary=--my-top-notch-boundary-";

			assertTrue("can parse ["+header+"]", MediaTypes.MULTIPART.isConsumable(acceptHeader));
		}
	}

	@Test
	public void defaultJsonCharsetUtf8() {
		assertEquals("json should not have utf-8 charset", "UTF-8", MediaTypes.JSON.getDefaultCharset().name());
	}

	@Test
	public void noCharsetOnOctetstreams() {
		assertNull("octet-stream should not have a charset", MediaTypes.OCTET.getDefaultCharset());
		assertNull("pdf should not have a charset", MediaTypes.PDF.getDefaultCharset());
	}
}
