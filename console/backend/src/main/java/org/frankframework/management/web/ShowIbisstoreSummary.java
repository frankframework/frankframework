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
package org.frankframework.management.web;

import java.util.Map;
import java.util.Map.Entry;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

@Path("/")
public final class ShowIbisstoreSummary extends FrankApiBase {

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/summary")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("jdbc")
	@Description("view database dump of the IbisStore table")
	public Response getIbisstoreSummary(Map<String, Object> json) {

		String query = null;
		String datasource = null;

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if("datasource".equalsIgnoreCase(key)) {
				datasource = entry.getValue().toString();
			}
			if("query".equalsIgnoreCase(key)) {
				query = entry.getValue().toString();
			}
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISSTORE_SUMMARY);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		builder.addHeader("query", query);
		return callSyncGateway(builder);
	}
}
