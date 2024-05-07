package org.frankframework.management.web.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * This is a test class to test the {@link FrankApiBase} class.
 * It tests path parameters, query parameters and json convertions
 */
public class ApiTestBaseTest extends FrankApiBase {

	@GetMapping(value = "/test/{path}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getWithPathAndQueryParam(@PathVariable("path") String path, @RequestParam(value = "bool", required = false) boolean bool) throws ApiException {
		return ResponseEntity.ok().body(new String[] { path, String.valueOf(bool)});
	}

	@PutMapping(value = "/test/put/json", produces = MediaType.APPLICATION_JSON_VALUE , consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> simplePut(LinkedHashMap<String, Object> json) throws ApiException {
		assertTrue(json.containsKey("two"), "key [two] must be present!");
		assertEquals("dos", json.get("two"));
		return ResponseEntity.ok().body(json);
	}

	@GetMapping(value = "/test/json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getWithJsonResponse() throws ApiException {
		Map<String, String> response = new HashMap<>();
		response.put("one", "uno");
		response.put("two", "dos");
		response.put("three", "tres");
		return ResponseEntity.ok().body(response);
	}

	@Nested
	public class TestApi extends FrankApiTestBase<ApiTestBaseTest> {

		@Override
		public ApiTestBaseTest createSpringMvcResource() {
			return new ApiTestBaseTest();
		}

		@Test
		public void testGetWithPathAndQueryParam() {
			ResponseEntity<?> response = dispatcher.dispatchRequest("GET", "/test/dummy?bool=false");
			String entity = (String) response.getBody();
			assertEquals("[\"dummy\",\"false\"]", entity);
		}

		@Test
		public void testGetWithJson() {
			ResponseEntity<?> response = dispatcher.dispatchRequest("GET", "/test/json");
			String entity = (String) response.getBody();
			assertEquals("{\"one\":\"uno\",\"two\":\"dos\",\"three\":\"tres\"}", entity);
		}

		@Test
		public void testPutJson() {
			ResponseEntity<?> response = dispatcher.dispatchRequest("PUT", "/test/put/json", "{\"one\":\"uno\",\"two\":\"dos\",\"three\":\"tres\"}");
			String entity = (String) response.getBody();
			assertEquals("{\"one\":\"uno\",\"two\":\"dos\",\"three\":\"tres\"}", entity);
		}
	}
}
