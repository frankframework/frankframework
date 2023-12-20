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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.UnsupportedCharsetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

public class MediaTypeTest {

	@Test
	public void fromValue() {
		//Test the 3 most commonly used mediaTypes
		assertEquals(MediaTypes.XML, MediaTypes.fromValue("application/xml"), "fromValue [XML]");
		assertEquals(MediaTypes.JSON, MediaTypes.fromValue("application/json"), "fromValue [JSON]");
		assertEquals(MediaTypes.TEXT, MediaTypes.fromValue("text/plain"), "fromValue [TEXT]");
	}

	@Test
	public void fromValueUnknown() {
		assertThrows(IllegalArgumentException.class, ()->MediaTypes.fromValue("something/unknown"));
	}

	@Test
	public void includesXML() {
		String acceptHeader = "application/xml";

		assertTrue(MediaTypes.XML.includes(acceptHeader), "can parse [XML]");
		assertFalse(MediaTypes.JSON.includes(acceptHeader), "should not be able to parse [JSON]");
	}

	@Test
	public void includesJSON() {
		String acceptHeader = "application/json";

		assertFalse(MediaTypes.XML.includes(acceptHeader), "should not be able to parse [XML]");
		assertTrue(MediaTypes.JSON.includes(acceptHeader), "can parse [JSON]");
	}

	@Test
	public void includesANY() {
		String acceptHeader = "application/octet-stream";

		assertTrue(MediaTypes.ANY.includes(acceptHeader), "can parse anything");
	}

	@ParameterizedTest
	@CsvSource({"application/*+xml", "application/xml;q=0.9", "*/*;q=0.8"})
	public void testWeightedXMLHeaders(String mimeType) {
		assertTrue(MediaTypes.XML.accepts(mimeType), "XML should accept weighted header ["+mimeType+"]");
		assertFalse(MediaTypes.XML.includes(mimeType), "Should not allow weighted header");
	}

	@ParameterizedTest
	@NullAndEmptySource
	public void isConsumableEmptyHeader(String mimeType) {
		// Act / Assert
		assertTrue(MediaTypes.ANY.includes(mimeType), "ANY accepts NULL or EMPTY value");
		assertFalse(MediaTypes.XML.includes(mimeType), "XML does not accept NULL or EMPTY value");
		assertFalse(MediaTypes.JSON.includes(mimeType), "JSON does not accept NULL or EMPTY value");
	}

	@ParameterizedTest
	@CsvSource({"multipart/form-data", "multipart/related", "multipart/mixed"})
	public void isConsumableMULTIPARTS(String header) {
		//There are different multipart contentTypes, see: https://msdn.microsoft.com/en-us/library/ms527355(v=exchg.10).aspx
		//Test at least the 3 most commonly used multiparts
		String acceptHeader = header + "; type=text; boundary=--my-top-notch-boundary-";

		assertTrue(MediaTypes.MULTIPART.includes(acceptHeader), "can parse ["+header+"]");
	}

	@Test
	public void defaultJsonCharsetUtf8() {
		assertEquals("UTF-8", MediaTypes.JSON.getDefaultCharset().name(), "json should not have utf-8 charset");
	}

	@Test
	public void noCharsetOnOctetstreams() {
		assertNull(MediaTypes.OCTET.getDefaultCharset(), "octet-stream should not have a charset");
		assertNull(MediaTypes.PDF.getDefaultCharset(), "pdf should not have a charset");
	}

	@Test
	public void toMimetype() {
		assertNull(MediaTypes.ANY.getDefaultCharset(), "MediaType should not have a charset by default");
		assertEquals("*/*", MediaTypes.ANY.getMimeType().toString(), "ContentType should be */* without charset");
	}

	@Test
	public void toMimetypeUtf8Charset() {
		assertEquals("UTF-8", MediaTypes.TEXT.getDefaultCharset().name(), "ContentType should have a charset UTF-8");
		assertEquals("text/plain;charset=UTF-8", MediaTypes.TEXT.getMimeType(null).toString(), "ContentType should be text/plain with utf-8 charset");
	}

	@Test
	public void toMimetypeIsoCharset() {
		assertEquals("text/plain;charset=ISO-8859-1", MediaTypes.TEXT.getMimeType("ISO-8859-1").toString());
	}

	@Test
	public void toMimetypeAsciiCharset() {
		assertEquals("text/plain;charset=US-ASCII", MediaTypes.TEXT.getMimeType("US-ASCII").toString());
	}

	@Test
	public void faultyCharset() {
		assertThrows(UnsupportedCharsetException.class, ()->MediaTypes.TEXT.getMimeType("ISO-1234"));
	}

	@Test
	public void mimeTypeDoesNotSupportCharset() {
		assertThrows(UnsupportedCharsetException.class, ()->MediaTypes.PDF.getMimeType("ISO-8859-1"));
	}
}
