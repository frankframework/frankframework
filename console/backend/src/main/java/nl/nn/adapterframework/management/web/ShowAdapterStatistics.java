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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;

/**
 * Retrieves the statistics
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowAdapterStatistics extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/adapters/{adapter}/statistics")
	@Relation("statistics")
	@Produces(MediaType.APPLICATION_JSON)
	@Deprecated
	public Response getStatisticsOld(@PathParam("adapter") String adapter, @QueryParam("configuration") String configuration) {
		final String config = (StringUtils.isNotEmpty(configuration)) ? configuration : null;
		return getStatistics(config, adapter);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/adapters/{adapter}/statistics")
	@Relation("statistics")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStatistics(@PathParam("configuration") String configuration, @PathParam("adapter") String adapter) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.ADAPTER, BusAction.STATUS);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		builder.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapter);
		return callSyncGateway(builder);
	}
}
