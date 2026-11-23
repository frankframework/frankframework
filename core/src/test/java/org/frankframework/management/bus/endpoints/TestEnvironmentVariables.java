/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.management.bus.endpoints;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import tools.jackson.databind.ObjectMapper;

import org.frankframework.management.bus.BusTestBase;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.testutil.SpringRootInitializer;
import org.frankframework.util.AppConstants;

@SpringJUnitConfig(initializers = {SpringRootInitializer.class})
@WithMockUser(roles = { "IbisTester" })
public class TestEnvironmentVariables extends BusTestBase {

	@Test
	public void testEnvironmentVariables() {
		MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.ENVIRONMENT);
		Message<String> jsonResponse = (Message<String>) callSyncGateway(request);

		assertThat(jsonResponse.getPayload(), Matchers.containsString("\"test.property\":\"one2drie\""));
	}

	@Test
	public void environmentVariableListedInPropertiesHideIsMasked() {
		Map.Entry<String, String> envVar = System.getenv().entrySet().stream()
				.filter(entry -> StringUtils.isNotEmpty(entry.getValue()))
				.findFirst()
				.orElse(null);
		assumeTrue(envVar != null, "no environment variable available to hide");

		Object originalPropertiesHide = AppConstants.getInstance().setProperty("properties.hide", envVar.getKey());
		try {
			MessageBuilder<String> request = createRequestMessage("NONE", BusTopic.ENVIRONMENT);
			Message<String> jsonResponse = (Message<String>) callSyncGateway(request);

			Map<String, Object> response = new ObjectMapper().readValue(jsonResponse.getPayload(), Map.class);
			Map<String, Object> environmentVariables = (Map<String, Object>) response.get("Environment Variables");

			assertEquals("*".repeat(envVar.getValue().length()), environmentVariables.get(envVar.getKey()));
		} finally {
			if (originalPropertiesHide == null) {
				AppConstants.getInstance().remove("properties.hide");
			} else {
				AppConstants.getInstance().setProperty("properties.hide", originalPropertiesHide.toString());
			}
		}
	}
}
