/*
Copyright 2021 WeAreFrank!

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

import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.jdbc.migration.Migrator;

@Path("/")
public final class ShowLiquibaseScript extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/liquibase")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConfigurations() throws ApiException {

		List<String> configNames= new ArrayList<String>();

		for(Configuration config : getIbisManager().getConfigurations()) {
			try(Migrator databaseMigrator = config.getBean("jdbcMigrator", Migrator.class)) {
				if(databaseMigrator.hasLiquibaseScript(config)) {
					configNames.add(config.getName());
				}
			}
		}

		HashMap<String, Object> resultMap = new HashMap<>();
		resultMap.put("configurationsWithLiquibaseScript", configNames);

		return Response.status(Response.Status.OK).entity(resultMap).build();
	}

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/liquibase")
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(MultipartBody inputDataMap) throws ApiException {

		Response.ResponseBuilder response = Response.noContent();
		InputStream file=null;
		if(inputDataMap.getAttachment("file") != null) {
			file = resolveTypeFromMap(inputDataMap, "file", InputStream.class, null);
		}
		String configuration = resolveStringFromMap(inputDataMap, "configuration", null);

		if(configuration == null && file == null) {
			return response.status(Response.Status.BAD_REQUEST).build();
		}

		String result = null;
		Writer writer = new StringBuilderWriter();
		Configuration config = getIbisManager().getConfiguration(configuration);
		try(Migrator databaseMigrator = config.getBean("jdbcMigrator", Migrator.class)) {
			if(file != null) {
				String fileName = inputDataMap.getAttachment("file").getContentDisposition().getParameter( "filename" );
				databaseMigrator.configure(file, fileName);
			} else {
				databaseMigrator.configure();
			}
			result = databaseMigrator.getUpdateSql(writer).toString();
		} catch (Exception e) {
			throw new ApiException("Error generating SQL script", e);
		}

		if(StringUtils.isEmpty(result)) {
			throw new ApiException("Make sure liquibase xml script exists for configuration ["+configuration+"]");
		}

		HashMap<String, Object> resultMap = new HashMap<>();
		resultMap.put("result", result);

		return Response.status(Response.Status.CREATED).entity(resultMap).build();
	}

}
