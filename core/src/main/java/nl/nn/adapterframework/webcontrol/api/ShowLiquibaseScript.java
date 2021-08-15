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

import java.io.Writer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.Configuration;
import nl.nn.adapterframework.jdbc.migration.Migrator;

@Path("/")
public final class ShowLiquibaseScript extends Base {

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/liquibase")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response execute(LinkedHashMap<String, Object> json) throws ApiException {

		Response.ResponseBuilder response = Response.noContent();

		String datasource = null;
		String configuration = null;
		for (Entry<String, Object> entry : json.entrySet()) {
			String key = entry.getKey();
			if("datasource".equalsIgnoreCase(key)) {
				datasource = entry.getValue().toString();
			}
			if("configuration".equalsIgnoreCase(key)) {
				configuration = entry.getValue().toString();
			}
		}

		if(datasource == null || configuration == null)
			return response.status(Response.Status.BAD_REQUEST).build();

		String result = null;
		Writer writer = new StringBuilderWriter();
		Configuration config = getIbisManager().getConfiguration(configuration);
		try(Migrator databaseMigrator = config.getBean("jdbcMigrator", Migrator.class)) {
			databaseMigrator.setIbisContext(getIbisContext());
			databaseMigrator.setDatasourceName(datasource);
			databaseMigrator.configure();
			result = databaseMigrator.getUpdateSql(writer).toString();
		} catch (Exception e) {
			throw new ApiException("Error generating SQL script", e);
		}

		if(StringUtils.isEmpty(result)) {
			throw new ApiException("Make sure liquibase xml script exists for configuration ["+configuration+"]");
		}

		HashMap<String, Object> resultMap = new HashMap<String, Object>();
		resultMap.put("result", result);

		return Response.status(Response.Status.CREATED).entity(resultMap).build();
	}

}
