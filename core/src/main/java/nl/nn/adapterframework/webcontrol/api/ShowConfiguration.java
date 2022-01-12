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
package nl.nn.adapterframework.webcontrol.api;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.IbisManager.IbisAction;
import nl.nn.adapterframework.core.IAdapter;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jndi.JndiDataSourceFactory;
import nl.nn.adapterframework.receivers.Receiver;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.NameComparatorBase;
import nl.nn.adapterframework.util.RunStateEnum;
import nl.nn.adapterframework.util.flow.FlowDiagramManager;

/**
 * Shows the configuration (with resolved variables).
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public final class ShowConfiguration extends Base {

	private String orderBy = AppConstants.getInstance().getProperty("iaf-api.configurations.orderby", "version").trim();

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations")
	@Produces(MediaType.APPLICATION_XML)
	public Response getXMLConfiguration(@QueryParam("loadedConfiguration") boolean loaded, @QueryParam("flow") String flow) throws ApiException {

		if(StringUtils.isNotEmpty(flow)) {
			FlowDiagramManager flowDiagramManager = getFlowDiagramManager();

			try {
				ResponseBuilder response = Response.status(Response.Status.OK);
				if("dot".equalsIgnoreCase(flow)) {
					response.entity(flowDiagramManager.generateDot(getIbisManager().getConfigurations())).type(MediaType.TEXT_PLAIN);
				} else {
					response.entity(flowDiagramManager.get(getIbisManager().getConfigurations())).type("image/svg+xml");
				}
				return response.build();
			} catch (SAXException | TransformerException | IOException e) {
				throw new ApiException(e);
			}
		}
		else {
			String result = "";
			for (Configuration configuration : getIbisManager().getConfigurations()) {
				if (loaded) {
					result = result + configuration.getLoadedConfiguration();
				} else {
					result = result + configuration.getOriginalConfiguration();
				}
			}
			return Response.status(Response.Status.OK).entity(result).build();
		}
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
					getIbisManager().handleAction(IbisAction.FULLRELOAD, "", "", "", getUserPrincipalName(), true);
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
	@PermitAll
	@Path("/configurations/{configuration}/health")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigurationHealth(@PathParam("configuration") String configurationName) throws ApiException {

		Configuration configuration = getIbisManager().getConfiguration(configurationName);

		if(configuration == null) {
			throw new ApiException("Configuration not found!");
		}
		if(!configuration.isActive()) {
			throw new ApiException("Configuration not active", configuration.getConfigurationException());
		}

		Map<String, Object> response = new HashMap<>();
		Map<RunStateEnum, Integer> stateCount = new HashMap<>();
		List<String> errors = new ArrayList<>();

		for (IAdapter adapter : configuration.getRegisteredAdapters()) {
			RunStateEnum state = adapter.getRunState(); //Let's not make it difficult for ourselves and only use STARTED/ERROR enums

			if(state.equals(RunStateEnum.STARTED)) {
				for (Receiver<?> receiver: adapter.getReceivers()) {
					RunStateEnum rState = receiver.getRunState();
	
					if(!rState.equals(RunStateEnum.STARTED)) {
						errors.add("receiver["+receiver.getName()+"] of adapter["+adapter.getName()+"] is in state["+rState.toString()+"]");
						state = RunStateEnum.ERROR;
					}
				}
			}
			else {
				errors.add("adapter["+adapter.getName()+"] is in state["+state.toString()+"]");
				state = RunStateEnum.ERROR;
			}

			int count;
			if(stateCount.containsKey(state))
				count = stateCount.get(state);
			else
				count = 0;

			stateCount.put(state, ++count);
		}

		Status status = Response.Status.OK;
		if(stateCount.containsKey(RunStateEnum.ERROR))
			status = Response.Status.SERVICE_UNAVAILABLE;

		if(!errors.isEmpty())
			response.put("errors", errors);
		response.put("status", status);

		return Response.status(status).entity(response).build();
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/flow")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getAdapterFlow(@PathParam("configuration") String configurationName, @QueryParam("dot") boolean dot) throws ApiException {

		Configuration configuration = getIbisManager().getConfiguration(configurationName);

		if(configuration == null){
			throw new ApiException("Configuration not found!");
		}

		FlowDiagramManager flowDiagramManager = getFlowDiagramManager();

		try {
			ResponseBuilder response = Response.status(Response.Status.OK);
			if(dot) {
				response.entity(flowDiagramManager.generateDot(configuration)).type(MediaType.TEXT_PLAIN);
			} else {
				response.entity(flowDiagramManager.get(configuration)).type("image/svg+xml");
			}
			return response.build();
		} catch (SAXException | TransformerException | IOException e) {
			throw new ApiException(e);
		}
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
					getIbisManager().handleAction(IbisAction.RELOAD, configurationName, "", "", getUserPrincipalName(), false);
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

		String version = Misc.urlDecode(encodedVersion);

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
	public Response uploadConfiguration(MultipartBody inputDataMap) throws ApiException {

		String fileName = null;
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		String datasource = resolveStringFromMap(inputDataMap, "datasource");
		boolean multiple_configs = resolveTypeFromMap(inputDataMap, "multiple_configs", boolean.class, false);
		boolean activate_config  = resolveTypeFromMap(inputDataMap, "activate_config", boolean.class, true);
		boolean automatic_reload = resolveTypeFromMap(inputDataMap, "automatic_reload", boolean.class, false);
		InputStream file = resolveTypeFromMap(inputDataMap, "file", InputStream.class, null);

		String user = resolveTypeFromMap(inputDataMap, "user", String.class, "");
		if(StringUtils.isEmpty(user)) {
			user = getUserPrincipalName();
		}

		fileName = inputDataMap.getAttachment("file").getContentDisposition().getParameter( "filename" );

		Map<String, String> result = new LinkedHashMap<String, String>();
		try {
			if(multiple_configs) {
				try {
					result = ConfigurationUtils.processMultiConfigZipFile(getIbisContext(), datasource, activate_config, automatic_reload, file, user);
				} catch (IOException e) {
					throw new ApiException(e);
				}
			} else {
				String configName=ConfigurationUtils.addConfigToDatabase(getIbisContext(), datasource, activate_config, automatic_reload, fileName, file, user);
				if(configName != null) {
					result.put(configName, "loaded");
				}
			}
			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception e) {
			throw new ApiException("Failed to upload Configuration", e);
		}
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/{configuration}/versions/{version}/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadConfiguration(@PathParam("configuration") String configurationName, @PathParam("version") String version, @QueryParam("dataSourceName") String dataSourceName) throws ApiException {

		if (StringUtils.isEmpty(version))
			version = null;
		if (StringUtils.isEmpty(dataSourceName))
			dataSourceName = null;

		try {
			Map<String, Object> configuration = ConfigurationUtils.getConfigFromDatabase(getIbisContext(), configurationName, dataSourceName, version);
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


	private List<Map<String, Object>> getConfigsFromDatabase(String configurationName, String dataSourceName) {
		List<Map<String, Object>> returnMap = new ArrayList<Map<String, Object>>();

		if (StringUtils.isEmpty(dataSourceName)) {
			dataSourceName = JndiDataSourceFactory.GLOBAL_DEFAULT_DATASOURCE_NAME;
			if (StringUtils.isEmpty(dataSourceName)) {
				return null;
			}
		}

		FixedQuerySender qs = getIbisContext().createBeanAutowireByName(FixedQuerySender.class);
		qs.setDatasourceName(dataSourceName);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		try {
			qs.configure();
			qs.open();
			try (Connection conn = qs.getConnection()) {
				String query = "SELECT NAME, VERSION, FILENAME, RUSER, ACTIVECONFIG, AUTORELOAD, CRE_TYDST FROM IBISCONFIG WHERE NAME=?";
				try (PreparedStatement stmt = conn.prepareStatement(query)) {
					stmt.setString(1, configurationName);
					try (ResultSet rs = stmt.executeQuery()) {
						while (rs.next()) {
							Map<String, Object> config = new HashMap<>();
							config.put("name", rs.getString(1));
							config.put("version", rs.getString(2));
							config.put("filename", rs.getString(3));
							config.put("user", rs.getString(4));
							config.put("active", rs.getBoolean(5));
							config.put("autoreload", rs.getBoolean(6));

							Date creationDate = rs.getDate(7);
							config.put("created", DateUtils.format(creationDate, DateUtils.FORMAT_GENERICDATETIME));
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

		returnMap.sort(new NameComparatorBase<Map<String, Object>>() {

			@Override
			public int compare(Map<String, Object> obj1, Map<String, Object> obj2) {
				String filename1 = (String) obj1.get(orderBy);
				String filename2 = (String) obj2.get(orderBy);

				return -compareNames(filename1, filename2); //invert the results as we want the latest version first
			}

		});
		return returnMap;
	}
}
