/*
Copyright 2020 WeAreFrank!

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
import static org.junit.Assert.assertNull;

import java.nio.charset.UnsupportedCharsetException;

import org.junit.Test;

public class ContentTypeTest {

	@Test
	public void defaultTest() {
		ContentType contentType = new ContentType(MediaTypes.ANY);
		assertNull("MediaType should not have a charset by default", contentType.getCharset());
		assertEquals("ContentType should be */* without charset", "*/*", contentType.getContentType());
	}

	@Test
	public void utf8Charset() {
		ContentType contentType = new ContentType(MediaTypes.TEXT);
		assertEquals("ContentType should have a charset UTF-8", "UTF-8", contentType.getCharset().name());
		assertEquals("ContentType should be application/pdf with utf-8 charset", "text/plain;charset=UTF-8", contentType.getContentType());
	}

	@Test
	public void isoCharset() {
		ContentType contentType = new ContentType(MediaTypes.TEXT);
		contentType.setCharset("ISO-8859-1");
		assertEquals("text/plain;charset=ISO-8859-1", contentType.getContentType());
	}

	@Test
	public void asciiCharset() {
		ContentType contentType = new ContentType(MediaTypes.TEXT);
		contentType.setCharset("US-ASCII");
		assertEquals("text/plain;charset=US-ASCII", contentType.getContentType());
	}

	@Test(expected=UnsupportedCharsetException.class)
	public void faultyCharset() {
		ContentType contentType = new ContentType(MediaTypes.TEXT);
		contentType.setCharset("ISO-1234");
	}
}
