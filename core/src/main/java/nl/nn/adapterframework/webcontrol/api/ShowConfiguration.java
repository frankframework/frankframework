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

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.nn.adapterframework.configuration.Configuration;

/**
* Shows the configuration (with resolved variables).
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowConfiguration extends Base {
	@Context ServletConfig servletConfig;

	@GET
	@RolesAllowed({"ObserverAccess", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations")
	@Produces(MediaType.APPLICATION_XML)
	public Response getConfiguration(@QueryParam("loadedConfiguration") boolean loadedConfiguration) throws ApiException {
		initBase(servletConfig);

		String result = "";

		for (Configuration configuration : ibisManager.getConfigurations()) {
			if (loadedConfiguration) {
				result = result + configuration.getOriginalConfiguration();
			} else {
				result = result + configuration.getLoadedConfiguration();
			}
		}

		return Response.status(Response.Status.CREATED).entity(result).build();
	}

	@GET
	@RolesAllowed({"ObserverAccess", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getConfigurationByName(@PathParam("configuration") String configurationName, @QueryParam("loadedConfiguration") boolean loadedConfiguration) throws ApiException {
		initBase(servletConfig);

		String result = "";

		Configuration configuration = ibisManager.getConfiguration(configurationName);
		if (loadedConfiguration) {
			result = configuration.getOriginalConfiguration();
		} else {
			result = configuration.getLoadedConfiguration();
		}

		return Response.status(Response.Status.CREATED).entity(result).build();
	}
}
