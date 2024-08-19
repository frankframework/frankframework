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
package org.frankframework.console.controllers;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusTopic;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConnectionOverview extends FrankApiBase {

	@AllowAllIbisUserRoles
	@GetMapping(value = "/connections", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getConnections() {
		return callSyncGateway(RequestMessageBuilder.create(BusTopic.CONNECTION_OVERVIEW));
	}

}
