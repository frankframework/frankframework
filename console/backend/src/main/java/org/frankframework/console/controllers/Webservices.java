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

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

import java.util.Map;

@RestController
public class Webservices {

	private final FrankApiService frankApiService;

	public Webservices(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@Relation("webservices")
	@Description("view a list of all available webservices")
	@GetMapping(value = "/webservices", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getWebServices() {
		return frankApiService.callSyncGateway(RequestMessageBuilder.create(BusTopic.WEBSERVICES, BusAction.GET));
	}

	@AllowAllIbisUserRoles
	@Relation("webservices")
	@Description("view OpenAPI specificiation")
	@GetMapping(value = "/webservices/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getOpenApiSpec(@RequestParam(value = "uri", required = false) String uri) {
		RequestMessageBuilder request = RequestMessageBuilder.create(BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.addHeader("type", "openapi");

		if (StringUtils.isNotBlank(uri)) {
			request.addHeader("uri", uri);
		}

		return frankApiService.callSyncGateway(request);
	}

	@AllowAllIbisUserRoles
	@Relation("webservices")
	@Description("view WDSL specificiation")
	@GetMapping(value = "/webservices/{configuration}/{resourceName}", produces = MediaType.APPLICATION_XML_VALUE)
	public ResponseEntity<?> getWsdl(@PathVariable("configuration") String configuration,
									 @PathVariable("resourceName") String resourceName,
									 @RequestParam Map<String, String> params) {
		RequestMessageBuilder request = RequestMessageBuilder.create(BusTopic.WEBSERVICES, BusAction.DOWNLOAD);

		request.addHeader("indent", params.getOrDefault("indent", "true"));
		request.addHeader("useIncludes", params.getOrDefault("useIncludes", "false"));
		request.addHeader("type", "wsdl");

		String adapterName;
		int dotPos = resourceName.lastIndexOf('.');
		if (dotPos >= 0) {
			adapterName = resourceName.substring(0, dotPos);
			boolean zip = ".zip".equals(resourceName.substring(dotPos));
			request.addHeader("zip", zip);
		} else {
			adapterName = resourceName;
		}

		if (StringUtils.isEmpty(adapterName)) {
			throw new ApiException("no adapter specified", HttpStatus.BAD_REQUEST);
		}

		request.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapterName);
		request.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);

		return frankApiService.callSyncGateway(request);
	}
}
