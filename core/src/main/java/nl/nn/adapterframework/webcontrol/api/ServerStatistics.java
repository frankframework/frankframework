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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
import nl.nn.adapterframework.util.AppConstants;

/**
 * Collection of server and application statistics and information.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class ServerStatistics extends Base {

	@GET
	@PermitAll
	@Path("/server/info")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerInformation() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.GET));
	}

	@GET
	@PermitAll
	@Path("/server/configurations")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAllConfigurations() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.FIND));
	}

	@GET
	@PermitAll
	@Path("/server/warnings")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getServerConfiguration() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.APPLICATION, BusAction.WARNINGS), true);
	}

	@GET
	@PermitAll
	@Path("/server/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getIbisHealth() {
		Map<String, Object> response = new HashMap<>();

		try {
			getIbisManager();
		}
		catch(ApiException e) {
			response.put("status", Response.Status.INTERNAL_SERVER_ERROR);
			response.put("error", e.getMessage());

			Throwable cause = e.getCause();
			if(cause != null && cause.getStackTrace() != null) {
				String dtapStage = AppConstants.getInstance().getString("dtap.stage", null);
				if((!"ACC".equals(dtapStage) && !"PRD".equals(dtapStage))) {
					response.put("stackTrace", cause.getStackTrace());
				}
			}

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		}
		catch(Exception e) {
			throw new ApiException(e);
		}


		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.HEALTH));
	}
}
