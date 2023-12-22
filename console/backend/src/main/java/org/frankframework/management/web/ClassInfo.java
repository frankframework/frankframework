/*
   Copyright 2022 WeAreFrank!

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

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

/**
 * API to get class information
 */
@Path("/")
public class ClassInfo extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/classinfo/{className}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getClassInfo(@PathParam("className") String className, @QueryParam("base") String baseClassName) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.DEBUG, BusAction.GET);
		builder.addHeader("className", className);
		builder.addHeader("baseClassName", baseClassName);
		return callSyncGateway(builder);
	}

}
