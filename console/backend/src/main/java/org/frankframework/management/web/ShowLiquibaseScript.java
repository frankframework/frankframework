/*
   Copyright 2021-2023 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.springframework.messaging.Message;

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusMessageUtils;
import org.frankframework.management.bus.BusTopic;

import org.frankframework.util.RequestUtils;
import org.frankframework.util.StreamUtil;

@Path("/")
public class ShowLiquibaseScript extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/liquibase")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadScript(@QueryParam("configuration") String configuration) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC_MIGRATION, BusAction.DOWNLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);
		return callSyncGateway(builder);
	}

	@POST
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/jdbc/liquibase")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response generateSQL(MultipartBody inputDataMap) throws ApiException {

		String configuration = RequestUtils.resolveStringFromMap(inputDataMap, "configuration", null);

		Attachment filePart = inputDataMap.getAttachment("file");
		if(configuration == null || filePart == null) {
			return Response.status(Response.Status.BAD_REQUEST).build();
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.JDBC_MIGRATION, BusAction.UPLOAD);
		builder.addHeader(BusMessageUtils.HEADER_CONFIGURATION_NAME_KEY, configuration);

		String filename = filePart.getContentDisposition().getParameter("filename");
		InputStream file = filePart.getObject(InputStream.class);
		try {
			String payload = StreamUtil.streamToString(file);
			builder.setPayload(payload);
		} catch (IOException e) {
			throw new ApiException("unable to read payload", e);
		}

		if (StringUtils.endsWithIgnoreCase(filename, ".zip")) {
			throw new ApiException("uploading zip files is not supported!");
		}
		builder.addHeader("filename", filename);
		Message<?> response = sendSyncMessage(builder);
		String result = (String) response.getPayload();

		if(StringUtils.isEmpty(result)) {
			throw new ApiException("Make sure liquibase xml script exists for configuration ["+configuration+"]");
		}

		HashMap<String, Object> resultMap = new HashMap<>();
		resultMap.put("result", result);

		return Response.status(Response.Status.CREATED).entity(resultMap).build();
	}

}
