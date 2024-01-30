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

import org.frankframework.management.bus.BusAction;
import org.frankframework.management.bus.BusTopic;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import java.io.OutputStreamWriter;

@Path("/")
public class FileViewer extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/file-viewer")
	@Produces()
	public Response getFileContent(@QueryParam("file") String file, @QueryParam("type") String type) {
		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.FILE_VIEWER, BusAction.GET);
		builder.addHeader("fileName", file);
		builder.addHeader("resultType", type);
		return processFileContents(builder, type);
	}

	// splitFileContents / streamFileContents?
	private Response processFileContents(RequestMessageBuilder builder, String wantedType) {
		if(wantedType.equalsIgnoreCase("html")){
			// TODO retrieve from bus and write as OutputStream to JaxRs while modifying each line
		}
		// TODO Check if JaxRs can handle streaming to the client and not just creating an entity and then sending the full data at once
		return callSyncGateway(builder);
	}

}
