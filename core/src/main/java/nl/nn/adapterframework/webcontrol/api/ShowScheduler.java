/*
Copyright 2016 Integration Partners B.V.

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
import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
* Retrieves the Scheduler metadata and the jobgroups with there jobs from the Scheduler.
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowScheduler extends Base {

	@GET
	@RolesAllowed({"ObserverAccess", "IbisTester"})
	@Path("/schedules")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSchedules(@Context ServletConfig servletConfig) throws ApiException {
		initBase(servletConfig);
		
		if (ibisManager == null) {
			throw new ApiException("Config not found!");
		}
		
		Map<String, Object> schedules = new HashMap<String, Object>();
		
		//Do code
		
		return Response.status(Response.Status.CREATED).entity(schedules).build();
	}
}
