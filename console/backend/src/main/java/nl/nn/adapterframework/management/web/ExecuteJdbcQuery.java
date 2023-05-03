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
package nl.nn.adapterframework.management.web;

import java.util.Map;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.util.RequestUtils;

/**
 * Executes a query.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ExecuteJdbcQuery extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getJdbcInfo() throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC, BusAction.GET);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({"IbisTester"})
	@Path("/jdbc/query")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(Map<String, Object> json) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC, BusAction.MANAGE);
		String datasource = RequestUtils.getValue(json, "datasource");
		String query = RequestUtils.getValue(json, "query");
		String resultType = RequestUtils.getValue(json, "resultType");

		if(resultType == null || query == null) {
			throw new ApiException("Missing data, datasource, resultType and query are expected.", 400);
		}
		builder.addHeader("query", query);
		builder.addHeader("resultType", resultType);

		builder.addHeader("avoidLocking", RequestUtils.getBooleanValue(json, "avoidLocking"));
		builder.addHeader("trimSpaces", RequestUtils.getBooleanValue(json, "trimSpaces"));

		String queryType = RequestUtils.getValue(json, "queryType");
		if("AUTO".equals(queryType)) {
			queryType = "other"; // defaults to other

			String[] commands = new String[] {"select", "show"}; //if it matches, set it to select
			for (String command : commands) {
				if(query.toLowerCase().startsWith(command)) {
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