package org.frankframework.console.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import org.frankframework.console.ApiException;

/**
 * This is a test class to test the {@link FrankApiService} class.
 * It tests path parameters, query parameters and json conversion
 */
@Controller
public class ApiTestBaseTest {

	@GetMapping(value = "/test/{path}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getWithPathAndQueryParam(@PathVariable("path") String path, @RequestParam(value = "bool", required = false) boolean bool) throws ApiException {
		return ResponseEntity.ok().body(new String[]{path, String.valueOf(bool)});
	}

	@PutMapping(value = "/test/put/json", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> simplePut(@RequestBody LinkedHashMap<String, Object> json) throws ApiException {
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
	@ContextConfiguration(classes = {WebTestConfiguration.class, ApiTestBaseTest.class})
	public class TestApi extends FrankApiTestBase {

		@Test
		public void testGetWithPathAndQueryParam() throws Exception {
			MvcResult result = mockMvc.perform(
							MockMvcRequestBuilders
									.get("/test/dummy")
									.param("bool", "false")
									.accept(MediaType.APPLICATION_JSON)
					)
					.andExpect(MockMvcResultMatchers.status().isOk())
					.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
					.andReturn();

			String response = result.getResponse().getContentAsString();
			assertEquals("[ \"dummy\", \"false\" ]", response);
		}

		@Test
		public void testGetWithJson() throws Exception {
			mockMvc.perform(MockMvcRequestBuilders.get("/test/json").accept(MediaType.APPLICATION_JSON))
					.andExpect(MockMvcResultMatchers.status().isOk())
					.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
					.andExpect(MockMvcResultMatchers.jsonPath("one").value("uno"))
					.andExpect(MockMvcResultMatchers.jsonPath("two").value("dos"))
					.andExpect(MockMvcResultMatchers.jsonPath("three").value("tres"));
		}

		@Test
		public void testPutJson() throws Exception {
			mockMvc.perform(
					MockMvcRequestBuilders
							.put("/test/put/json")
							.content("{\"one\":\"uno\",\"two\":\"dos\",\"three\":\"tres\"}")
							.contentType(MediaType.APPLICATION_JSON)
							.accept(MediaType.APPLICATION_JSON)
					)
					.andExpect(MockMvcResultMatchers.status().isOk())
					.andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON))
					.andExpect(MockMvcResultMatchers.jsonPath("one").value("uno"))
					.andExpect(MockMvcResultMatchers.jsonPath("two").value("dos"))
					.andExpect(MockMvcResultMatchers.jsonPath("three").value("tres"));
		}
	}
}
