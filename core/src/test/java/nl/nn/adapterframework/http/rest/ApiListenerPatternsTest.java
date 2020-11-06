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

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;


@RunWith(Parameterized.class)
public class ApiListenerPatternsTest {

	private ApiListener listener;
	private String expectedUriPattern;
	private String expectedCleanPattern;

	@Parameters(name = "inputUriPattern[{0}] -> expectedUriPattern[{1}] -> expectedCleanPattern[{2}]")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {
				{ "*", "/*", "/*" },
				{ "test", "/test", "/test" },
				{ "/test", "/test", "/test" },
				{ "/test/", "/test", "/test" },

				{ "test/*", "/test/*", "/test/*" },
				{ "*/*", "/*/*", "/*/*" },
				{ "test/something", "/test/something", "/test/something" },
				{ "test/*/something", "/test/*/something", "/test/*/something" },
				{ "test/*/*", "/test/*/*", "/test/*/*" },
				{ "test/*/something/else", "/test/*/something/else", "/test/*/something/else" },
				{ "test/*/something/*", "/test/*/something/*", "/test/*/something/*" },

				{ "/*/*/*", "/*/*/*", "/*/*/*" },

				{ "/text/{name}", "/text/{name}", "/text/*" },
				{ "/text/{name}/something", "/text/{name}/something", "/text/*/something" },
				{ "/text/{name}/{name2}", "/text/{name}/{name2}", "/text/*/*" },
		});
	}

	public ApiListenerPatternsTest(String pattern, String expectedUriPattern, String expectedCleanPattern) {
		listener = new ApiListener();
		listener.setName("my-api-listener");
		listener.setMethod("put");
		listener.setUriPattern(pattern);
		this.expectedUriPattern = expectedUriPattern;
		this.expectedCleanPattern = expectedCleanPattern;
	}

	@Test
	public void testUriPattern() {
		assertEquals(expectedUriPattern, listener.getUriPattern());
	}

	@Test
	public void testCleanPattern() {
		assertEquals(expectedCleanPattern, listener.getCleanPattern());
	}
}
