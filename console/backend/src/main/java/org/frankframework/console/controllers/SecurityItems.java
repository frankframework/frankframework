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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@RestController
public class SecurityItems {

	private final FrankApiService frankApiService;

	public SecurityItems(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@GetMapping(value = "/securityitems", produces = MediaType.APPLICATION_JSON_VALUE)
	@Relation("securityitems")
	public ResponseEntity<?> getSecurityItems() {
		if (frankApiService.hasNoAvailableWorker()) {
			return ResponseEntity.status(503).body(getUnavailableSecurityItems());
		}
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.SECURITY_ITEMS);
		return frankApiService.callSyncGateway(builder);
	}

	private Map<String, Object> getUnavailableSecurityItems() {
		Map<String, Object> returnMap = new HashMap<>();
		returnMap.put("securityRoles", getSecurityRoles());
		returnMap.put("jmsRealms", Map.of());
		returnMap.put("resourceFactories", List.of());
		returnMap.put("sapSystems", List.of());
		returnMap.put("authEntries", List.of());
		returnMap.put("xmlComponents", Map.of());
		returnMap.put("supportedConnectionOptions", Map.of());
		returnMap.put("expiringCertificates", List.of());
		return returnMap;
	}

	private List<SecurityRolesDTO> getSecurityRoles() {
		return BusMessageUtils.getAuthorities()
			.stream()
			.map(SecurityRolesDTO::new)
			.toList();
	}

	private static class SecurityRolesDTO {
		private final @Getter String name;
		private final @Getter boolean allowed;

		public SecurityRolesDTO(GrantedAuthority authority) {
			this.name = authority.getAuthority().substring(5);
			this.allowed = BusMessageUtils.hasRole(this.name);
		}
	}
}
