/*
   Copyright 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.management.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class TestLoggingSettings extends FrankApiTestBase<UpdateLoggingConfig> {

	@Override
	public UpdateLoggingConfig createJaxRsResource() {
		return new UpdateLoggingConfig();
	}

	@Test
	public void updateLogIntermediaryResults() {
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"logIntermediaryResults\":\"true\"}");
		assertEquals(200, response.getStatus()); //void method does not return anything
	}

	@Test
	public void updateMaxLength() {
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"maxMessageLength\":\"50\"}");
		assertEquals(200, response.getStatus()); //void method does not return anything
	}

	@Test
	public void updateTesttoolEnabled() {
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"enableDebugger\":\"false\"}");
		assertEquals(200, response.getStatus()); //void method does not return anything
	}

	@Test
	public void updateLogLevel() {
		Response response = dispatcher.dispatchRequest(HttpMethod.PUT, "/server/logging", "{\"loglevel\":\"dummy\"}");
		assertEquals(200, response.getStatus()); //void method does not return anything
	}
}
