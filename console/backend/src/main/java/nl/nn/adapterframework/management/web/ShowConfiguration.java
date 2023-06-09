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

import java.io.InputStream;
import java.util.LinkedHashMap;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import nl.nn.adapterframework.management.IbisAction;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusMessageUtils;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.util.HttpUtils;
import nl.nn.adapterframework.util.RequestUtils;

/**
 * Shows the configuration (with resolved variables).
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowConfiguration extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations")
	@Produces(MediaType.APPLICATION_XML)
	public Response getXMLConfiguration(@QueryParam("loadedConfiguration") boolean loaded, @QueryParam("flow") String flow) throws ApiException {
		if(StringUtils.isNotEmpty(flow)) {
			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FLOW);
			return callSyncGateway(builder);
		} else {
			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.GET);
			if(loaded) builder.addHeader("loaded", loaded);
			return callSyncGateway(builder);
		}
	}

	@PUT
	@RolesAllowed({"IbisAdmin", "IbisTester"})
	@Path("/configurations")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response fullReload(LinkedHashMap<String, Object> json) throws ApiException {
		Object value = json.get("action");
		if(value instanceof String && "reload".equals(value)) {
			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
			builder.addHeader("action", IbisAction.FULLRELOAD.name());
			callAsyncGateway(builder);
			return Response.status(Response.Status.ACCEPTED).entity("{\"status\":\"ok\"}").build();
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getConfigurationByName(@PathParam("configuration") String configurationName, @QueryParam("loadedConfiguration") boolean loaded) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.GET);
		builder.addHeader("configuration", configurationName);
		if(loaded) builder.addHeader("loaded", loaded);
		return callSyncGateway(builder);
	}

	@GET
	@PermitAll
	@Path("/configurations/{configuration}/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigurationHealth(@PathParam("configuration") String configurationName) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.HEALTH);
		builder.addHeader("configuration", configurationName);
		return callSyncGateway(builder);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/flow")
	public Response getConfigurationFlow(@PathParam("configuration") String configurationName) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FLOW);
		builder.addHeader("configuration", configurationName);
		return callSyncGateway(builder);
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response reloadConfiguration(@PathParam("configuration") String configurationName, LinkedHashMap<String, Object> json) throws ApiException {
		Object value = json.get("action");
		if(value instanceof String && "reload".equals(value)) {
			RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.IBISACTION);
			builder.addHeader("action", IbisAction.RELOAD.name());
			builder.addHeader("configuration", configurationName);
			callAsyncGateway(builder);
			return Response.status(Response.Status.ACCEPTED).entity("{\"status\":\"ok\"}").build();
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/versions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigurationDetailsByName(@PathParam("configuration") String configurationName, @QueryParam("datasourceName") String datasourceName) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.FIND);
		builder.addHeader("configuration", configurationName);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasourceName);
		return callSyncGateway(builder);
	}

	@PUT
	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	@Path("/configurations/{configuration}/versions/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response manageConfiguration(@PathParam("configuration") String configurationName, @PathParam("version") String encodedVersion, @QueryParam("datasourceName") String datasourceName, LinkedHashMap<String, Object> json) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.MANAGE);
		builder.addHeader("configuration", configurationName);
		builder.addHeader("version", HttpUtils.urlDecode(encodedVersion));
		if(json.containsKey("activate")) {
			builder.addHeader("activate", json.get("activate"));
		} else if(json.containsKey("autoreload")) {
			builder.addHeader("autoreload", json.get("autoreload"));
		}
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasourceName);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	@Path("configurations")
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadConfiguration(MultipartBody inputDataMap) throws ApiException {
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		String datasource = RequestUtils.resolveStringFromMap(inputDataMap, "datasource", "");
		boolean multipleConfigs = RequestUtils.resolveTypeFromMap(inputDataMap, "multiple_configs", boolean.class, false);
		boolean activateConfig  = RequestUtils.resolveTypeFromMap(inputDataMap, "activate_config", boolean.class, true);
		boolean automaticReload = RequestUtils.resolveTypeFromMap(inputDataMap, "automatic_reload", boolean.class, false);
		InputStream file = RequestUtils.resolveTypeFromMap(inputDataMap, "file", InputStream.class, null);

		String user = RequestUtils.resolveTypeFromMap(inputDataMap, "user", String.class, "");
		if(StringUtils.isEmpty(user)) {
			user = getUserPrincipalName();
		}

		String fileName = inputDataMap.getAttachment("file").getContentDisposition().getParameter( "filename" );

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.UPLOAD);
		builder.setPayload(file);
		builder.addHeader("filename", fileName);
		builder.addHeader("multiple_configs", multipleConfigs);
		builder.addHeader("activate_config", activateConfig);
		builder.addHeader("automatic_reload", automaticReload);
		builder.addHeader("user", user);
		if(StringUtils.isNotEmpty(datasource)) {
			builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasource);
		}
		return callSyncGateway(builder);
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/versions/{version}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadConfiguration(@PathParam("configuration") String configurationName, @PathParam("version") String version, @QueryParam("dataSourceName") String dataSourceName) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.DOWNLOAD);
		builder.addHeader("configuration", configurationName);
		builder.addHeader("version", version);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, dataSourceName);
		return callSyncGateway(builder);
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/versions/{version}")
	public Response deleteConfiguration(@PathParam("configuration") String configurationName, @PathParam("version") String version, @QueryParam("datasourceName") String datasourceName) throws ApiException {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.CONFIGURATION, BusAction.DELETE);
		builder.addHeader("configuration", configurationName);
		builder.addHeader("version", version);
		builder.addHeader(BusMessageUtils.HEADER_DATASOURCE_NAME_KEY, datasourceName);
		return callAsyncGateway(builder);
	}
}
