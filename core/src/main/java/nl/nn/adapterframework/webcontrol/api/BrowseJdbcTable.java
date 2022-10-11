/*
Copyright 2016-2022 WeAreFrank!

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
package nl.nn.adapterframework.webcontrol.api;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;

@Path("/")
public final class BrowseJdbcTable extends FrankApiBase {

	@POST
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/browse")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException {
		String datasource = null, tableName = null, where = "", order = "";
		Boolean numberOfRowsOnly = false;
		int minRow = 1, maxRow = 100;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if(key.equalsIgnoreCase("datasource")) {
				datasource = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("table")) {
				tableName = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("where")) {
				where = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("order")) { // the form field named 'order' is only used for 'group by', when number of rows only is true.
				order = entry.getValue().toString();
			}
			if(key.equalsIgnoreCase("numberOfRowsOnly")) {
				numberOfRowsOnly = Boolean.parseBoolean(entry.getValue().toString());
			}
			if(key.equalsIgnoreCase("minRow") && entry.getValue() != "") {
				minRow = Integer.parseInt(entry.getValue().toString());
				minRow = Math.max(minRow, 0);
			}
			if(key.equalsIgnoreCase("maxRow") && entry.getValue() != "") {
				maxRow = Integer.parseInt(entry.getValue().toString());
				maxRow = Math.max(maxRow, 1);
			}
		}

		if(datasource == null || tableName == null) {
			throw new ApiException("datasource and/or tableName not defined.", 400);
		}

		if(maxRow < minRow)
			throw new ApiException("Rownum max must be greater than or equal to Rownum min", 400);
		if (maxRow - minRow >= 100) {
			throw new ApiException("Difference between Rownum max and Rownum min must be less than hundred", 400);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC, BusAction.FIND);
		builder.addHeader(HEADER_DATASOURCE_NAME_KEY, datasource);
		builder.addHeader("table", tableName);
		builder.addHeader("where", where);
		builder.addHeader("order", order);
		builder.addHeader("numberOfRowsOnly", numberOfRowsOnly);
		builder.addHeader("minRow", minRow);
		builder.addHeader("maxRow", maxRow);
		return callSyncGateway(builder);
	}
}
