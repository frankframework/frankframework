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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@RestController
public class IbisstoreSummary {

	private final FrankApiService frankApiService;

	public IbisstoreSummary(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@Relation("jdbc")
	@Description("view database dump of the IbisStore table")
	@PostMapping(value = "/jdbc/summary", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getIbisStoreSummary(@RequestBody IbisStoreSummaryModel model) {
		String query = model.query;
		String datasource = model.datasource;

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.IBISSTORE_SUMMARY);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		builder.addHeader("query", query);
		return frankApiService.callSyncGateway(builder);
	}

	public record IbisStoreSummaryModel(String query, String datasource) {}

}
