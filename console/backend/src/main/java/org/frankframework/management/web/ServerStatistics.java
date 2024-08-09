/*
   Copyright 2024 WeAreFrank!

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

import jakarta.annotation.security.PermitAll;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server")
public class ServerStatistics extends FrankApiBase {

	@PermitAll
	@GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerInformation() {
		return callSyncGateway(RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.GET));
	}

	@PermitAll
	@GetMapping(value = "/configurations", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAllConfigurations() {
		return callSyncGateway(RequestMessageBuilder.create(BusTopic.CONFIGURATION, BusAction.FIND));
	}

	@PermitAll
	@GetMapping(value = "/warnings", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerConfiguration() {
		return callSyncGateway(RequestMessageBuilder.create(BusTopic.APPLICATION, BusAction.WARNINGS));
	}

	@PermitAll
	@GetMapping(value = "/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getFrankHealth() {
		return callSyncGateway(RequestMessageBuilder.create(BusTopic.HEALTH));
	}
}
