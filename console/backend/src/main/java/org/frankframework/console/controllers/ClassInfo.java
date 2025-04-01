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

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

@RestController
public class ClassInfo {

	private final FrankApiService frankApiService;

	public ClassInfo(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@GetMapping(value = "/classinfo/{className}", produces = MediaType.APPLICATION_JSON_VALUE)
	@Relation("debug")
	@Description("view a specific class introspection")
	public ResponseEntity<?> getClassInfo(PathVariableModel pathVariables, ParametersModel params) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.DEBUG, BusAction.GET);
		builder.addHeader("className", pathVariables.className);
		builder.addHeader("baseClassName", params.base);
		return frankApiService.callSyncGateway(builder);
	}

	public record PathVariableModel(String className) {}
	public record ParametersModel(String base) {}
}
