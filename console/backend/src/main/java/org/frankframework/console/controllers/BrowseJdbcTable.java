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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@RestController
public class BrowseJdbcTable {

	private final FrankApiService frankApiService;

	public BrowseJdbcTable(FrankApiService frankApiService) {
		this.frankApiService = frankApiService;
	}

	@AllowAllIbisUserRoles
	@PostMapping(value = "/jdbc/browse", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Relation("jdbc")
	@Description("view a specific JDBC table")
	public ResponseEntity<?> browseJdbcTable(@RequestBody BrowseJdbcModel json) {
		if (json.table == null) {
			throw new ApiException("tableName not defined.", 400);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.JDBC, BusAction.FIND);
		if (StringUtils.isNotEmpty(json.datasource)) {
			builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, json.datasource);
		}
		builder.addHeader("table", json.table);
		builder.addHeader("where", json.where);
		builder.addHeader("order", json.order);
		builder.addHeader("numberOfRowsOnly", json.numberOfRowsOnly);
		builder.addHeader("minRow", json.minRow);
		builder.addHeader("maxRow", json.maxRow);
		return frankApiService.callSyncGateway(builder);
	}

	public record BrowseJdbcModel(
			String datasource,
			String table,
			String where,
			String order,
			Boolean numberOfRowsOnly,
			Integer minRow,
			Integer maxRow
	) { }
}
