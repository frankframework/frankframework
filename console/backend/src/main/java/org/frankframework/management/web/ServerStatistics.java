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

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.security.PermitAll;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ServerStatistics extends FrankApiBase {

	@PermitAll
	@GetMapping(value = "/server/info", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerInformation() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.GET));
	}

	@PermitAll
	@GetMapping(value = "/server/configurations", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getAllConfigurations() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.FIND));
	}

	@PermitAll
	@GetMapping(value = "/server/warnings", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getServerConfiguration() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.WARNINGS));
	}

	@PermitAll
	@GetMapping(value = "/server/health", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getFrankHealth() {
		try {
			return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.HEALTH));
		} catch(ApiException e) {
			Map<String, Object> response = new HashMap<>();
			response.put("status", HttpStatus.INTERNAL_SERVER_ERROR);
			response.put("error", "unable to connect to backend system: "+e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		} catch (MessageHandlingException e) {
			throw e; //Spring gateway exchange exceptions are handled by the SpringBusExceptionHandler
		} catch (Exception e) {
			throw new ApiException(e);
		}
	}

}
