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

import java.util.Map;

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
import org.frankframework.console.util.RequestUtils;
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
	public ResponseEntity<?> browseJdbcTable(@RequestBody Map<String, Object> json) {
		String datasource = RequestUtils.getValue(json, "datasource");
		String tableName = RequestUtils.getValue(json, "table");
		String where = RequestUtils.getValue(json, "where");
		String order = RequestUtils.getValue(json, "order");
		Boolean numberOfRowsOnly = RequestUtils.getBooleanValue(json, "numberOfRowsOnly");

		Integer minRow = RequestUtils.getIntegerValue(json, "minRow");
		Integer maxRow = RequestUtils.getIntegerValue(json, "maxRow");

		if (tableName == null) {
			throw new ApiException("tableName not defined.", 400);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.JDBC, BusAction.FIND);
		if (StringUtils.isNotEmpty(datasource)) {
			builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		}
		builder.addHeader("table", tableName);
		builder.addHeader("where", where);
		builder.addHeader("order", order);
		builder.addHeader("numberOfRowsOnly", numberOfRowsOnly);
		builder.addHeader("minRow", minRow);
		builder.addHeader("maxRow", maxRow);
		return frankApiService.callSyncGateway(builder);
	}
}
