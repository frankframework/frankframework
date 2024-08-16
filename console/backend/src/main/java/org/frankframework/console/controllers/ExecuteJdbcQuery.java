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

import jakarta.annotation.security.RolesAllowed;

import org.frankframework.console.AllowAllIbisUserRoles;
import org.frankframework.console.ApiException;
import org.frankframework.console.Description;
import org.frankframework.console.Relation;
import org.frankframework.console.util.RequestMessageBuilder;
import org.frankframework.console.util.RequestUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecuteJdbcQuery extends FrankApiBase {

	@AllowAllIbisUserRoles
	@GetMapping(value = "/jdbc", produces = MediaType.APPLICATION_JSON_VALUE)
	@Relation("jdbc")
	@Description("view a list of all JDBC DataSources")
	public ResponseEntity<?> getJdbcDataSources() throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.JDBC, BusAction.GET);
		return callSyncGateway(builder);
	}

	@RolesAllowed({"IbisTester"})
	@PostMapping(value = "/jdbc/query", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Relation("jdbc")
	@Description("execute a JDBC query on a datasource")
	public ResponseEntity<?> executeJdbcQuery(@RequestBody Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(BusTopic.JDBC, BusAction.MANAGE);
		String datasource = RequestUtils.getValue(json, "datasource");
		String query = RequestUtils.getValue(json, "query");
		String resultType = RequestUtils.getValue(json, "resultType");

		if (resultType == null || query == null) {
			throw new ApiException("Missing data, datasource, resultType and query are expected.", 400);
		}
		builder.addHeader("query", query);
		builder.addHeader("resultType", resultType);

		builder.addHeader("avoidLocking", RequestUtils.getBooleanValue(json, "avoidLocking"));
		builder.addHeader("trimSpaces", RequestUtils.getBooleanValue(json, "trimSpaces"));

		String queryType = RequestUtils.getValue(json, "queryType");
		if ("AUTO".equals(queryType)) {
			queryType = "other"; // defaults to other

			String[] commands = new String[]{"select", "show"}; //if it matches, set it to select
			for (String command : commands) {
				if (query.toLowerCase().startsWith(command)) {
					queryType = "select";
					break;
				}
			}
		}

		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		builder.addHeader("queryType", queryType);
		return callSyncGateway(builder);
	}

}
