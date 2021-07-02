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
package nl.nn.adapterframework.webcontrol.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class ServerStatisticsTest extends ApiTestBase<ServerStatistics> {

	@Override
	public ServerStatistics createJaxRsResource() {
		return new ServerStatistics();
	}

	@Test
	public void testServerInfo() {
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/server/info");
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertNotNull(response.getEntity());

		String result = response.getEntity().toString();
		assertThat(result, CoreMatchers.containsString("\"fileSystem\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"framework\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"instance\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"configurations\":[")); //Array
		assertThat(result, CoreMatchers.containsString("\"applicationServer\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"javaVersion\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"dtap.stage\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"dtap.side\":\"")); //String
		assertThat(result, CoreMatchers.containsString("\"processMetrics\":{")); //Object
		assertThat(result, CoreMatchers.containsString("\"machineName\":\"")); //String
	}

	@Test
	public void testServerHealth() {
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/server/health");
		assertEquals(503, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertEquals("{\"errors\":[\"adapter[dummyAdapter] is in state[STOPPED]\"],\"status\":\"SERVICE_UNAVAILABLE\"}", response.getEntity());
	}

	@Test
	public void testServerWarnings() {
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/server/warnings");
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertEquals("{\"TestConfiguration\":{\"errorStoreCount\":0,\"warnings\":[\"hello I am a configuration warning!\"]},\"totalErrorStoreCount\":0}", response.getEntity());
	}
}
