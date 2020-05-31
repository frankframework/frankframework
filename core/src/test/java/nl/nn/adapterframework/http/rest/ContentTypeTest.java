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

import org.junit.Test;

public class ContentTypeTest {

	@Test
	public void defaultTest() {
		ContentType contenType = new ContentType(MediaTypes.ANY);
		assertNull("MediaType should not have a charset by default", contenType.getCharset());
		assertEquals("ContentType should be */* without charset", "*/*", contenType.getContentType());
	}

	@Test
	public void withCharset() {
		ContentType contenType = new ContentType(MediaTypes.PDF);
		assertNull("ContentType should not have a charset by default", contenType.getCharset());
		assertEquals("ContentType should be application/pdf without charset", "application/pdf", contenType.getContentType());

		contenType.setCharset("Utf-8");
		assertEquals("application/pdf;charset=UTF-8", contenType.getContentType());
	}
}
