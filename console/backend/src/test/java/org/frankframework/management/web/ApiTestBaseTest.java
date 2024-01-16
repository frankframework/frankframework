/*
   Copyright 2020-2023 WeAreFrank!

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
package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * This is a test class to test the {@link FrankApiBase} class.
 * It tests path parameters, query parameters and json convertions
 */
public class ApiTestBaseTest extends FrankApiBase {

	@GET
	@Path("/test/{path}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWithPathAndQueryParam(@PathParam("path") String path, @QueryParam("bool") boolean bool) throws ApiException {
		return Response.ok().entity(new String[] { path, bool+""}).build();
	}

	@PUT
	@Path("/test/put/json")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response simplePut(LinkedHashMap<String, Object> json) throws ApiException {
		assertTrue(json.containsKey("two"), "key [two] must be present!");
		assertEquals("dos", json.get("two"));
		return Response.ok().entity(json).build();
	}

	@GET
	@Path("/test/json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWithJsonResponse() throws ApiException {
		Map<String, String> response = new HashMap<>();
		response.put("one", "uno");
		response.put("two", "dos");
		response.put("three", "tres");
		return Response.ok().entity(response).build();
	}

	@Nested
	public class TestApi extends FrankApiTestBase<ApiTestBaseTest> {

		@Override
		public ApiTestBaseTest createJaxRsResource() {
			return new ApiTestBaseTest();
		}

		@Test
		public void testGetWithPathAndQueryParam() {
			Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/test/dummy?bool=false");
			String entity = (String) response.getEntity();
			assertEquals("[\"dummy\",\"false\"]", entity);
		}

		@Test
		public void testGetWithJson() {
			Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/test/json");
			String entity = (String) response.getEntity();
			assertEquals("{\"one\":\"uno\",\"two\":\"dos\",\"three\":\"tres\"}", entity);
		}

		@Test
		public void testPutJson() {
			Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/test/put/json", "{\"one\":\"uno\",\"two\":\"dos\",\"three\":\"tres\"}");
			String entity = (String) response.getEntity();
			assertEquals("{\"one\":\"uno\",\"two\":\"dos\",\"three\":\"tres\"}", entity);
		}
	}
}
