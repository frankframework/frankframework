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
package nl.nn.adapterframework.management.web;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DefaultValue;
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
	@Relation("webservices")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getWebServices() {
		return callSyncGateway(RequestMessageBuilder.create(this, BusTopic.WEBSERVICES, BusAction.GET));
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices/openapi.json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getOpenApiSpec(@QueryParam("uri") String uri, @QueryParam("configuration") String configuration) {
		RequestMessageBuilder request = RequestMessageBuilder.create(this, BusTopic.WEBSERVICES, BusAction.DOWNLOAD);
		request.addHeader("type", "openapi");
		if(StringUtils.isNotBlank(uri)) {
			request.addHeader("uri", uri);
		}
		if(StringUtils.isNotBlank(configuration)) {
			request.addHeader("configuration", configuration);
		}
		return callSyncGateway(request);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/webservices/{configuration}/{resourceName}")
	@Relation("webservices")
	@Produces(MediaType.APPLICATION_XML)
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
			boolean zip = resourceName.substring(dotPos).equals(".zip");
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
