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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.frankframework.management.bus.BusTopic;

/**
 * Shows the environment variables.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowEnvironmentVariables extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/environmentvariables")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("debug")
	@Description("view all system/environment/application properties")
	public Response getEnvironmentVariables() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.ENVIRONMENT));
	}
}
