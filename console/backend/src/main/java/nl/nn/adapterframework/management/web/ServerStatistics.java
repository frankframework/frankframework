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
package nl.nn.adapterframework.management.web;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.messaging.MessageHandlingException;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;

/**
 * Collection of server and application statistics and information.
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class ServerStatistics extends FrankApiBase {

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
		try {
			return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.HEALTH));
		} catch(ApiException e) {
			Map<String, Object> response = new HashMap<>();
			response.put("status", Response.Status.INTERNAL_SERVER_ERROR);
			response.put("error", "unable to connect to backend system: "+e.getMessage());
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(response).build();
		} catch (MessageHandlingException e) {
			throw e; //Spring gateway exchange exceptions are handled by the SpringBusExceptionHandler
		} catch (Exception e) {
			throw new ApiException(e);
		}
	}
}
