/*
   Copyright 2024 WeAreFrank!

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

import org.apache.commons.lang3.StringUtils;
import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;
import org.frankframework.util.ResponseUtils;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

@Path("/")
public class FileViewer extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/file-viewer")
	@Produces({"text/html", "text/plain", "application/xml", "application/zip", "application/octet-stream"})
	@Relation("logging")
	@Description("view or download a (log)file")
	public Response getFileContent(@QueryParam("file") String file, @HeaderParam("Accept") String acceptHeader) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FILE_VIEWER, BusAction.GET);
		if (StringUtils.isEmpty(acceptHeader)) acceptHeader = "*/*";
		String acceptType = acceptHeader.split(",")[0];
		String wantedType = MediaType.valueOf(acceptType).getSubtype();
		builder.addHeader("fileName", file);
		builder.addHeader("resultType", wantedType);

		if (wantedType.equalsIgnoreCase("html")) {
			return processHtmlMessage(builder);
		}
		return callSyncGateway(builder);
	}

	private Response processHtmlMessage(RequestMessageBuilder builder) {
		Message<?> fileContentsMessage = sendSyncMessage(builder);
		StreamingOutput stream = outputStream -> {
			BufferedReader fileContentsReader = new BufferedReader(new InputStreamReader((InputStream) fileContentsMessage.getPayload()));
			String line;
			while ((line = fileContentsReader.readLine()) != null) {
				String formattedLine = StringUtils.replace(line, "\t", "&nbsp;&nbsp;&nbsp;&nbsp;");
				outputStream.write((formattedLine + "<br>").getBytes());
			}
			outputStream.flush();
		};
		return ResponseUtils.convertToJaxRsStreamingResponse(fileContentsMessage, stream).build();
	}

}
