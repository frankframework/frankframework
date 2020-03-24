/*
Copyright 2016-2020 Integration Partners B.V.

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

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsRealmFactory;

/**
 * Shows the configuration (with resolved variables).
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowConfiguration extends Base {
	@Context SecurityContext securityContext;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations")
	@Produces(MediaType.APPLICATION_XML)
	public Response getXMLConfiguration(@QueryParam("loadedConfiguration") boolean loaded, @QueryParam("flow") boolean flow) throws ApiException {

		String result = "";

		if(flow) {
			result = getFlow(getIbisManager().getConfigurations());
		}
		else {
			for (Configuration configuration : getIbisManager().getConfigurations()) {
				if (loaded) {
					result = result + configuration.getLoadedConfiguration();
				} else {
					result = result + configuration.getOriginalConfiguration();
				}
			}
		}

		return Response.status(Response.Status.OK).entity(result).build();
	}

	@PUT
	@RolesAllowed({"IbisAdmin", "IbisTester"})
	@Path("/configurations")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response fullReload(LinkedHashMap<String, Object> json) throws ApiException {

		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {
				if(value.equals("reload")) {
					getIbisManager().handleAdapter("FULLRELOAD", "", "", "", null, true);
				}
				response.entity("{\"status\":\"ok\"}");
			}
		}

		return response.build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}")
	@Produces(MediaType.APPLICATION_XML)
	public Response getConfigurationByName(@PathParam("configuration") String configurationName, @QueryParam("loadedConfiguration") boolean loadedConfiguration) throws ApiException {

		String result = "";

		Configuration configuration = getIbisManager().getConfiguration(configurationName);

		if(configuration == null){
			throw new ApiException("Configuration not found!");
		}

		if (loadedConfiguration) {
			result = configuration.getLoadedConfiguration();
		} else {
			result = configuration.getOriginalConfiguration();
		}

		return Response.status(Response.Status.OK).entity(result).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/flow")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getAdapterFlow(@PathParam("configuration") String configurationName) throws ApiException {

		Configuration configuration = getIbisManager().getConfiguration(configurationName);

		if(configuration == null){
			throw new ApiException("Configuration not found!");
		}

		return Response.status(Response.Status.OK).entity(getFlow(configuration)).build();
	}

	@PUT
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response reloadConfiguration(@PathParam("configuration") String configurationName, LinkedHashMap<String, Object> json) throws ApiException {

		Configuration configuration = getIbisManager().getConfiguration(configurationName);

		if(configuration == null){
			throw new ApiException("Configuration not found!");
		}

		Response.ResponseBuilder response = Response.status(Response.Status.NO_CONTENT); //PUT defaults to no content

		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			if(key.equalsIgnoreCase("action")) {
				if(value.equals("reload")) {
					getIbisManager().handleAdapter("RELOAD", configurationName, "", "", null, false);
				}
				response.entity("{\"status\":\"ok\"}");
			}
		}

		return response.build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/versions")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigurationDetailsByName(@PathParam("configuration") String configurationName, @QueryParam("realm") String jmsRealm) throws ApiException {

		Configuration configuration = getIbisManager().getConfiguration(configurationName);
		if(configuration == null) {
			throw new ApiException("Configuration not found!");
		}

		if ("DatabaseClassLoader".equals(configuration.getClassLoaderType())) {
			List<Map<String, Object>> configs = getConfigsFromDatabase(configurationName, jmsRealm);
			if(configs == null)
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();

			for(Map<String, Object> config: configs) {
				if(config.get("version").toString().equals(configuration.getVersion()))
					config.put("loaded", true);
				else
					config.put("loaded", false);
			}
			return Response.status(Response.Status.OK).entity(configs).build();
		}

		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@PUT
	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	@Path("/configurations/{configuration}/versions/{version}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response manageConfiguration(@PathParam("configuration") String configurationName, @PathParam("version") String encodedVersion, @QueryParam("realm") String jmsRealm, LinkedHashMap<String, Object> json) throws ApiException {

		Configuration configuration = getIbisManager().getConfiguration(configurationName);

		if(configuration == null){
			throw new ApiException("Configuration not found!");
		}

		String version = null;
		try {
			version = URLDecoder.decode(encodedVersion, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new ApiException("unable to decode encodedVersion ["+encodedVersion+"] with charset [UTF-8]", e);
		}

		try {
			for (Entry<String, Object> entry : json.entrySet()) {
				String key = entry.getKey();
				Object valueObject = entry.getValue();
				boolean value = false;
				if(valueObject instanceof Boolean) {
					value = (boolean) valueObject;
				}
				else
					value = Boolean.parseBoolean(valueObject.toString());

				if(key.equalsIgnoreCase("activate")) {
					if(ConfigurationUtils.activateConfig(getIbisContext(), configurationName, version, value, jmsRealm)) {
						return Response.status(Response.Status.ACCEPTED).build();
					}
				}
				else if(key.equalsIgnoreCase("autoreload")) {
					if(ConfigurationUtils.autoReloadConfig(getIbisContext(), configurationName, version, value, jmsRealm)) {
						return Response.status(Response.Status.ACCEPTED).build();
					}
				}
			}
		}
		catch (Exception e) {
			throw new ApiException(e);
		}

		return Response.status(Response.Status.BAD_REQUEST).build();
	}

	@POST
	@RolesAllowed({"IbisTester", "IbisAdmin", "IbisDataAdmin"})
	@Path("configurations")
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadConfiguration(MultipartFormDataInput input) throws ApiException {

		String datasource = null, fileName = null;
		InputStream file = null;
		boolean multiple_configs = false, activate_config = true, automatic_reload = false;
		Map<String, List<InputPart>> inputDataMap = input.getFormDataMap();
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		try {
			datasource = resolveStringFromMap(inputDataMap, "datasource");

			if(inputDataMap.get("multiple_configs") != null)
				multiple_configs = inputDataMap.get("multiple_configs").get(0).getBody(boolean.class, null);
			if(inputDataMap.get("activate_config") != null)
				activate_config = inputDataMap.get("activate_config").get(0).getBody(boolean.class, null);
			if(inputDataMap.get("automatic_reload") != null)
				automatic_reload = inputDataMap.get("automatic_reload").get(0).getBody(boolean.class, null);
			if(inputDataMap.get("file") != null)
				file = inputDataMap.get("file").get(0).getBody(InputStream.class, null);
			else
				throw new ApiException("No file specified", 400);

			MultivaluedMap<String, String> headers = inputDataMap.get("file").get(0).getHeaders();
			String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
			for (String fName : contentDispositionHeader) {
				if ((fName.trim().startsWith("filename"))) {
					String[] tmp = fName.split("=");
					fileName = tmp[1].trim().replaceAll("\"","");
				}
			}
		}
		catch (IOException e) {
			throw new ApiException("Failed to parse one or more parameters", e);
		}

		try {
			String user = null;
			Principal principal = securityContext.getUserPrincipal();
			if(principal != null)
				user = principal.getName();

			if(multiple_configs) {
				try {
					ConfigurationUtils.processMultiConfigZipFile(getIbisContext(), datasource, activate_config, automatic_reload, file, user);
				} catch (IOException e) {
					throw new ApiException(e);
				}
			} else {
				ConfigurationUtils.addConfigToDatabase(getIbisContext(), datasource, activate_config, automatic_reload, fileName, file, user);
			}

			return Response.status(Response.Status.CREATED).build();
		} catch (Exception e) {
			throw new ApiException("Failed to upload Configuration", e);
		}
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/versions/{version}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadConfiguration(@PathParam("configuration") String configurationName, @PathParam("version") String version, @QueryParam("realm") String jmsRealm) throws ApiException {

		if (StringUtils.isEmpty(version))
			version = null;
		if (StringUtils.isEmpty(jmsRealm))
			jmsRealm = null;

		try {
			Map<String, Object> configuration = ConfigurationUtils.getConfigFromDatabase(getIbisContext(), configurationName, jmsRealm, version);
			return Response
					.status(Response.Status.OK)
					.entity(configuration.get("CONFIG"))
					.header("Content-Disposition", "attachment; filename=\"" + configuration.get("FILENAME") + "\"")
					.build();
		} catch (Exception e) {
			throw new ApiException(e);
		}
	}

	@DELETE
	@RolesAllowed({"IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/versions/{version}")
	public Response deleteConfiguration(@PathParam("configuration") String configurationName, @PathParam("version") String version, @QueryParam("realm") String jmsRealm) throws ApiException {

		if (StringUtils.isEmpty(jmsRealm))
			jmsRealm = null;

		try {
			ConfigurationUtils.removeConfigFromDatabase(getIbisContext(), configurationName, jmsRealm, version);
			return Response.status(Response.Status.OK).build();
		} catch (Exception e) {
			throw new ApiException(e);
		}
	}


	private List<Map<String, Object>> getConfigsFromDatabase(String configurationName, String jmsRealm) {
		List<Map<String, Object>> returnMap = new ArrayList<Map<String, Object>>();

		if (StringUtils.isEmpty(jmsRealm)) {
			jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(jmsRealm)) {
				return null;
			}
		}

		FixedQuerySender qs = (FixedQuerySender) getIbisContext().createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		try {
			qs.configure();
			qs.open();
			try (Connection conn = qs.getConnection()) {
				String query = "SELECT NAME, VERSION, FILENAME, RUSER, ACTIVECONFIG, AUTORELOAD, CRE_TYDST FROM IBISCONFIG WHERE NAME=? ORDER BY CRE_TYDST";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, configurationName);
					try (ResultSet rs = stmt.executeQuery()) {
						while (rs.next()) {
							Map<String, Object> config = new HashMap<String, Object>();
							config.put("name", rs.getString(1));
							config.put("version", rs.getString(2));
							config.put("filename", rs.getString(3));
							config.put("user", rs.getString(4));
							config.put("active", rs.getBoolean(5));
							config.put("autoreload", rs.getBoolean(6));
							config.put("created", rs.getString(7));
							returnMap.add(config);
						}
					}
				}
			}
		} catch (Exception e) {
			throw new ApiException(e);
		} finally {
			qs.close();
		}
		return returnMap;
	}
}
