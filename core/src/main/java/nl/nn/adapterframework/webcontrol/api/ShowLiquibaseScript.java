/*
   Copyright 2021-2022 WeAreFrank!

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
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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
import nl.nn.adapterframework.configuration.IbisManager;
import nl.nn.adapterframework.core.BytesResource;
import nl.nn.adapterframework.jdbc.migration.DatabaseMigratorBase;
import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
import nl.nn.adapterframework.util.StreamUtil;

@Path("/")
public final class ShowLiquibaseScript extends Base {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/liquibase/download")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadScript() {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC_MIGRATION, BusAction.DOWNLOAD);
		builder.addHeader("configuration", IbisManager.ALL_CONFIGS_KEY);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/liquibase")
	@Produces(MediaType.APPLICATION_JSON)
	public Response generateSQL(MultipartBody inputDataMap) throws ApiException {

		Response.ResponseBuilder response = Response.noContent();
		InputStream file=null;
		if(inputDataMap.getAttachment("file") != null) {
			file = resolveTypeFromMap(inputDataMap, "file", InputStream.class, null);
		}
		String configuration = resolveStringFromMap(inputDataMap, "configuration", null);

		if(configuration == null && file == null) {
			return response.status(Response.Status.BAD_REQUEST).build();
		}

		Writer writer = new StringBuilderWriter();
		Configuration config = getIbisManager().getConfiguration(configuration);
		try {
			DatabaseMigratorBase databaseMigrator = config.getBean("jdbcMigrator", DatabaseMigratorBase.class);
			if(file != null) {
				String filename = inputDataMap.getAttachment("file").getContentDisposition().getParameter( "filename" );

				if (filename.endsWith(".xml")) {
					databaseMigrator.update(writer, new BytesResource(file, filename, config));
				} else {
					try(ZipInputStream stream = new ZipInputStream(file)){
						ZipEntry entry;
						while((entry = stream.getNextEntry()) != null) {
							databaseMigrator.update(writer, new BytesResource(StreamUtil.dontClose(stream), entry.getName(), config));
						}
					}
				}
			} else {
				databaseMigrator.update(writer);
			}
		} catch (Exception e) {
			throw new ApiException("Error generating SQL script", e);
		}
		String result = writer.toString();
		if(StringUtils.isEmpty(result)) {
			throw new ApiException("Make sure liquibase xml script exists for configuration ["+configuration+"]");
		}

		HashMap<String, Object> resultMap = new HashMap<>();
		resultMap.put("result", result);

		return Response.status(Response.Status.CREATED).entity(resultMap).build();
	}

}
