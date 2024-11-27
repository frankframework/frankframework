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

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.http.rest.ApiListener.HttpMethod;


public class ApiListenerPatternsTest {

	private ApiListener listener;

	public static Collection<Object[]> data() {
		return List.of(new Object[][] {
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

	public void initApiListenerPatternsTest(String pattern, String expectedUriPattern, String expectedCleanPattern) {
		listener = new ApiListener();
		listener.setName("my-api-listener");
		listener.setMethod(HttpMethod.PUT);
		listener.setUriPattern(pattern);
	}

	@MethodSource("data")
	@ParameterizedTest(name = "inputUriPattern[{0}] -> expectedUriPattern[{1}] -> expectedCleanPattern[{2}]")
	void testUriPattern(String pattern, String expectedUriPattern, String expectedCleanPattern) {
		initApiListenerPatternsTest(pattern, expectedUriPattern, expectedCleanPattern);
		assertEquals(expectedUriPattern, listener.getUriPattern());
	}

	@MethodSource("data")
	@ParameterizedTest(name = "inputUriPattern[{0}] -> expectedUriPattern[{1}] -> expectedCleanPattern[{2}]")
	void testCleanPattern(String pattern, String expectedUriPattern, String expectedCleanPattern) {
		initApiListenerPatternsTest(pattern, expectedUriPattern, expectedCleanPattern);
		assertEquals(expectedCleanPattern, listener.getCleanPattern());
	}
}
