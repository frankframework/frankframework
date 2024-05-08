/*
   Copyright 2016-2023 WeAreFrank!

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

import java.util.Map;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

import org.frankframework.util.RequestUtils;

@Path("/")
public final class BrowseJdbcTable extends FrankApiBase {

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/browse")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("jdbc")
	@Description("view a specific JDBC table")
	public Response browseJdbcTable(Map<String, Object> json) {
		String datasource = RequestUtils.getValue(json, "datasource");
		String tableName = RequestUtils.getValue(json, "table");
		String where = RequestUtils.getValue(json, "where");
		String order = RequestUtils.getValue(json, "order");
		Boolean numberOfRowsOnly = RequestUtils.getBooleanValue(json, "numberOfRowsOnly");

		Integer minRow = RequestUtils.getIntegerValue(json, "minRow");
		Integer maxRow = RequestUtils.getIntegerValue(json, "maxRow");

		if(tableName == null) {
			throw new ApiException("tableName not defined.", 400);
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC, BusAction.FIND);
		if(StringUtils.isNotEmpty(datasource)) {
			builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		}
		builder.addHeader("table", tableName);
		builder.addHeader("where", where);
		builder.addHeader("order", order);
		builder.addHeader("numberOfRowsOnly", numberOfRowsOnly);
		builder.addHeader("minRow", minRow);
		builder.addHeader("maxRow", maxRow);
		return callSyncGateway(builder);
	}
}
