/*
   Copyright 2016-2021 WeAreFrank!

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

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

/**
 * Shows all monitors.
 *
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class Webservices extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("webservices")
	@Description("view a list of all available webservices")
	public Response getWebServices() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.WEBSERVICES, BusAction.GET));
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices/openapi.json")
	@Produces(MediaType.APPLICATION_JSON)
	@Relation("webservices")
	@Description("view OpenAPI specificiation")
	public Response getOpenApiSpec(@QueryParam("uri") String uri) {
		RequestMessageBuilder request = RequestMessageBuilder.create(this, BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.addHeader("type", "openapi");
		if(StringUtils.isNotBlank(uri)) {
			request.addHeader("uri", uri);
		}
		return callSyncGateway(request);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices/{configuration}/{resourceName}")
	@Produces(MediaType.APPLICATION_XML)
	@Relation("webservices")
	@Description("view WDSL specificiation")
	public Response getWsdl(
			@PathParam("configuration") String configuration,
			@PathParam("resourceName") String resourceName,
			@DefaultValue("true") @QueryParam("indent") boolean indent,
			@DefaultValue("false") @QueryParam("useIncludes") boolean useIncludes) {

		RequestMessageBuilder request = RequestMessageBuilder.create(this, BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.addHeader("indent", indent);
		request.addHeader("useIncludes", useIncludes);
		request.addHeader("type", "wsdl");

		String adapterName;
		int dotPos=resourceName.lastIndexOf('.');
		if (dotPos>=0) {
			adapterName = resourceName.substring(0,dotPos);
			boolean zip = ".zip".equals(resourceName.substring(dotPos));
			request.addHeader("zip", zip);
		} else {
			adapterName = resourceName;
		}

		if (StringUtils.isEmpty(adapterName)) {
			throw new ApiException("no adapter specified");
		}

		request.addHeader(BusMessageUtils.HEADER_ADAPTER_NAME_KEY, adapterName);
		request.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		return callSyncGateway(request);
	}
}
