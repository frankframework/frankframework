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
package org.frankframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class ServerStatisticsTest extends FrankApiTestBase<ServerStatistics> {

	@Override
	public ServerStatistics createJaxRsResource() {
		return new ServerStatistics();
	}

	@Test
	@Disabled
	public void testServerWarnings() {
		Response response = dispatcher.dispatchRequest(HttpMethod.GET, "/server/warnings");
		assertEquals(200, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getMediaType().toString());

		assertEquals("{\"TestConfiguration\":{\"errorStoreCount\":0,\"warnings\":[\"hello I am a configuration warning!\"]},\"totalErrorStoreCount\":0}", response.getEntity());
	}
}
