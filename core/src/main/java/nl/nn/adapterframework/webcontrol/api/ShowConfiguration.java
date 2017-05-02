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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.security.Principal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.annotation.security.RolesAllowed;
import javax.servlet.ServletConfig;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationUtils;
import nl.nn.adapterframework.configuration.classloaders.DatabaseClassLoader;
import nl.nn.adapterframework.core.IMessageBrowser;
import nl.nn.adapterframework.core.IMessageBrowsingIterator;
import nl.nn.adapterframework.core.IMessageBrowsingIteratorItem;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.jdbc.FixedQuerySender;
import nl.nn.adapterframework.jms.JmsMessageBrowser;
import nl.nn.adapterframework.jms.JmsRealmFactory;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;
import nl.nn.adapterframework.util.XmlUtils;
import nl.nn.adapterframework.webcontrol.pipes.UploadConfig;

/**
* Shows the configuration (with resolved variables).
* 
* @author	Niels Meijer
*/

@Path("/")
public final class ShowConfiguration extends Base {
	@Context ServletConfig servletConfig;
	@Context SecurityContext securityContext;

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations")
	@Produces(MediaType.APPLICATION_XML)
	public Response getXMLConfiguration(@QueryParam("loadedConfiguration") boolean loadedConfiguration) throws ApiException {
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
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
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

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/manage/{configuration}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigurationDetailsByName(@PathParam("configuration") String configurationName, @QueryParam("realm") String jmsRealm) throws ApiException {
		initBase(servletConfig);

		Configuration configuration = ibisManager.getConfiguration(configurationName);
		if(configuration == null) {
			throw new ApiException("Configuration not found!");
		}

		if(configuration.getClassLoader().getParent() instanceof DatabaseClassLoader) {
			return Response.status(Response.Status.CREATED).entity(getConfigFromDatabase(configurationName, jmsRealm)).build();
		}

		return Response.status(Response.Status.NO_CONTENT).build();
	}

	@POST
	@RolesAllowed({"IbisTester"})
	@Path("configurations")
	@Produces(MediaType.APPLICATION_JSON)
	public Response uploadConfiguration(MultipartFormDataInput input) throws ApiException {

		initBase(servletConfig);

		String jmsRealm = null, name = null, version = null, fileName = null, fileEncoding = Misc.DEFAULT_INPUT_STREAM_ENCODING;
		InputStream file = null;
		boolean multiple_configs = false, activate_config = true, automatic_reload = false;
		Map<String, List<InputPart>> inputDataMap = input.getFormDataMap();
		if(inputDataMap == null) {
			throw new ApiException("Missing post parameters");
		}

		try {
			if(inputDataMap.get("realm") != null)
				jmsRealm = inputDataMap.get("realm").get(0).getBodyAsString();
			else
				throw new ApiException("JMS realm not defined", 400);
			if(inputDataMap.get("name") != null)
				name = inputDataMap.get("name").get(0).getBodyAsString();
			else
				throw new ApiException("No name specified", 400);
			if(inputDataMap.get("file_encoding") != null)
				fileEncoding = inputDataMap.get("file_encoding").get(0).getBodyAsString();
			if(inputDataMap.get("version") != null) 
				version = inputDataMap.get("version").get(0).getBodyAsString();
			else
				throw new ApiException("No version specified", 400);
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
			throw new ApiException("Failed to parse one or more parameters!");
		}

		try {
			String result = "";
			FixedQuerySender qs = (FixedQuerySender) ibisContext.createBeanAutowireByName(FixedQuerySender.class);
			if(multiple_configs) {
				if (StringUtils.isEmpty(name) && StringUtils.isEmpty(version)) {
					String[] fnArray = splitFilename(fileName);
					if (fnArray[0] != null) {
						name = fnArray[0];
					}
					if (fnArray[1] != null) {
						version = fnArray[1];
					}
				}
			}
			String user = null;
			Principal principal = securityContext.getUserPrincipal();
			if(principal != null)
				user = ""+principal;

			if(multiple_configs) {
				try {
					result = processZipFile(file, fileEncoding, fileName, automatic_reload, automatic_reload, user);
				} catch (IOException e) {
					throw new ApiException(e);
				}
			} else {
				ConfigurationUtils.addConfigToDatabase(ibisContext, jmsRealm, activate_config, automatic_reload, name, version, fileName, file, user);
			}

			return Response.status(Response.Status.CREATED).entity(result).build();
		} catch (Exception e) {
			throw new ApiException("Failed to upload Configuration!");
		}
	}

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/configurations/download/{configuration}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadConfiguration(@PathParam("configuration") String configurationName) throws ApiException {
		initBase(servletConfig);

		try {
			Map<String, Object> configuration = ConfigurationUtils.getConfigFromDatabase(ibisContext, configurationName);
			return Response
					.status(Response.Status.OK)
					.entity(configuration.get("CONFIG"))
					.header("Content-Disposition", "attachment; filename=\"" + configuration.get("FILENAME") + "\"")
					.build();
		} catch (Exception e) {
			throw new ApiException("Could not find configuration!");
		}
	}

	private List<Map<String, Object>> getConfigFromDatabase(String configurationName, String jmsRealm) {
		List<Map<String, Object>> returnMap = new ArrayList<Map<String, Object>>();

		if (StringUtils.isEmpty(jmsRealm)) {
			jmsRealm = JmsRealmFactory.getInstance().getFirstDatasourceJmsRealm();
			if (StringUtils.isEmpty(jmsRealm)) {
				return null;
			}
		}

		Connection conn = null;
		ResultSet rs = null;
		FixedQuerySender qs = (FixedQuerySender)ibisContext.createBeanAutowireByName(FixedQuerySender.class);
		qs.setJmsRealm(jmsRealm);
		qs.setQuery("SELECT COUNT(*) FROM IBISCONFIG");
		try {
			qs.configure();
			qs.open();
			conn = qs.getConnection();
			String query = "SELECT NAME, VERSION, FILENAME, RUSER, ACTIVECONFIG, CRE_TYDST FROM IBISCONFIG WHERE NAME=? ORDER BY CRE_TYDST";
			PreparedStatement stmt = conn.prepareStatement(query);
			stmt.setString(1, configurationName);
			rs = stmt.executeQuery();
			while (rs.next()) {
				Map<String, Object> config = new HashMap<String, Object>();
				config.put("name", rs.getString(1));
				config.put("version", rs.getString(2));
				config.put("filename", rs.getString(3));
				config.put("user", rs.getString(4));
				config.put("active", rs.getBoolean(5));
				config.put("created", rs.getString(6));
				returnMap.add(config);
			}
		} catch (Exception e) {
			throw new ApiException(e);
		} finally {
			qs.close();
			if (rs != null) {
				try {
					rs.close();
				} catch (SQLException e) {
					log.warn("Could not close resultset", e);
				}
			}
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					log.warn("Could not close connection", e);
				}
			}
		}
		return returnMap;
	}

	private String[] splitFilename(String fileName) {
		String name = null;
		String version = null;
		if (StringUtils.isNotEmpty(fileName)) {
			int i = fileName.lastIndexOf(".");
			if (i != -1) {
				name = fileName.substring(0, i);
				int j = name.lastIndexOf("-");
				if (j != -1) {
					name = name.substring(0, j);
					j = name.lastIndexOf("-");
					if (j != -1) {
						name = fileName.substring(0, j);
						version = fileName.substring(j + 1, i);
					}
				}
			}
		}
		return new String[] { name, version };
	}

	private String processZipFile(InputStream inputStream, String fileEncoding, String jmsRealm, boolean automatic_reload, boolean activate_config, String user) throws Exception {
		String result = "";
		if (inputStream.available() > 0) {
			ZipInputStream archive = new ZipInputStream(inputStream);
			int counter = 1;
			for (ZipEntry entry = archive.getNextEntry(); entry != null; entry = archive.getNextEntry()) {
				String entryName = entry.getName();
				int size = (int) entry.getSize();
				if (size > 0) {
					byte[] b = new byte[size];
					int rb = 0;
					int chunk = 0;
					while (((int) size - rb) > 0) {
						chunk = archive.read(b, rb, (int) size - rb);
						if (chunk == -1) {
							break;
						}
						rb += chunk;
					}
					ByteArrayInputStream bais = new ByteArrayInputStream(b, 0, rb);
					String fileName = "file_zipentry" + counter;
					if (StringUtils.isNotEmpty(result)) {
						result += "\n";
					}
					String name = "";
					String version = "";
					String[] fnArray = splitFilename(entryName);
					if (fnArray[0] != null) {
						name = fnArray[0];
					}
					if (fnArray[1] != null) {
						version = fnArray[1];
					}
					result += entryName + ":" + 
					ConfigurationUtils.addConfigToDatabase(ibisContext, jmsRealm, activate_config, automatic_reload, name, version, fileName, bais, user);
				}
				archive.closeEntry();
				counter++;
			}
			archive.close();
		}
		return result;
	}
}
